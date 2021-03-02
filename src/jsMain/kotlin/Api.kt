@file:OptIn(ExperimentalTime::class)

import api.AdditionalProblemProperties
import api.NameAvailability
import api.Problem
import api.ProblemInfo
import api.Solution
import api.SubmissionResult
import api.Toast
import api.ToastKind
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.websocket.DefaultClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocket
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import io.ktor.http.isSecure
import io.ktor.http.isWebsocket
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import kotlin.js.Date
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.seconds

object Api {
    private val client = HttpClient {
        install(JsonFeature) { serializer = KotlinxSerializer() }
        install(WebSockets)
        defaultRequest {
            url {
                host = window.location.hostname
                window.location.port.let { p -> if (p.isNotEmpty()) port = p.toInt() }
                if (!protocol.isWebsocket())
                    protocol = URLProtocol.createOrDefault(window.location.protocol.dropLast(1))
            }
        }
    }

    private suspend fun connectWS(path: String, block: suspend DefaultClientWebSocketSession.() -> Unit) {
        val proto = URLProtocol.createOrDefault(window.location.protocol.dropLast(1))
        val wsProtocol = if (proto.isSecure()) URLProtocol.WSS else URLProtocol.WS
        client.webSocket(path, { url { protocol = wsProtocol }; build() }) {
            block()
        }
    }

    private suspend fun postRequest(path: String, block: HttpRequestBuilder.() -> Unit = {}) =
        client.post<HttpResponse>(path, block)

    private suspend fun getRequest(path: String, block: HttpRequestBuilder.() -> Unit = {}) =
        client.get<HttpResponse>(path, block)

    suspend fun getProblems(): List<Problem> = getRequest("problems").receive()

    suspend fun getProblemInfo(problemId: Int): ProblemInfo = getRequest("problems/$problemId").receive()

    suspend fun downloadPackage(problem: Problem, props: AdditionalProblemProperties) {
        val fullName = props.buildFullName(problem.name)
        val bytes = postRequest("problems/${problem.id}/download") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            body = props
            parameter("full-name", fullName)
        }.readBytes()
        downloadZip(bytes, "$fullName.zip")
    }

    suspend fun transferToBacsArchive(problem: Problem, props: AdditionalProblemProperties) {
        val fullName = props.buildFullName(problem.name)
        postRequest("problems/${problem.id}/transfer") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            body = props
            parameter("full-name", fullName)
        }
    }

    suspend fun getNameAvailability(name: String): NameAvailability {
        return getRequest("problems/get-name-availability") {
            parameter("name", name)
        }.receive()
    }

    suspend fun registerNotifications() {
        val delayDuration = 1.seconds
        mainScope.launch {
            var first = true
            while (true) {
                try {
                    console.log("Registering session")
                    getRequest("register-session")
                    console.log("Session registered")
                    console.log("Connecting to WS")
                    connectWS("subscribe") {
                        console.log("Connected to WS")
                        if (!first)
                            showToast(
                                Toast(
                                    "Соединение",
                                    "Соединение восстановлено. " +
                                        "Возможно, стоит перезагрузить страницу, чтобы подгрузить обновления",
                                    ToastKind.SUCCESS
                                )
                            )
                        incoming.consumeAsFlow()
                            .mapNotNull { it as? Frame.Text }
                            .collect { showToast(Json.decodeFromString(it.readText())) }
                    }
                } catch (e: Throwable) {
                    console.log("Exception occurred: ${e.message}")
                    e.printStackTrace()
                } finally {
                    first = false
                    showToast(
                        Toast(
                            "Соединение",
                            "Проблемы с соединением. Попробую переподключиться через $delayDuration",
                            ToastKind.FAILURE
                        )
                    )
                    console.log("Disconnected from WS. Reconnect in $delayDuration")
                    delay(delayDuration)
                }
            }
        }
    }

    suspend fun getSolutions(problem: Problem): List<Solution> {
        return getRequest("problems/${problem.id}/solutions") {
            parameter("name", problem.name)
        }.receive()
    }

    suspend fun prepareProblem(problem: Problem): Int {
        var sybonProblemId = -1
        connectWS("problems/${problem.id}/solutions/prepare") {
            sybonProblemId = (incoming.receive() as Frame.Text).readText().toInt()
        }
        return sybonProblemId
    }

    suspend fun testSolution(problem: Problem, sybonProblemId: Int, solutionName: String): SubmissionResult {
        var result: SubmissionResult? = null
        connectWS("problems/${problem.id}/solutions/test") {
            console.log("Started testing $solutionName at ${Date()}")
            val duration = measureTime {
                send("$sybonProblemId")
                send(solutionName)
                result = Json.decodeFromString<SubmissionResult>((incoming.receive() as Frame.Text).readText())
            }
            console.log("Tested $solutionName at ${Date()} for $duration")
        }
        return result!!
    }

    // https://stackoverflow.com/a/30832210/4296219
    private fun downloadZip(content: ByteArray, filename: String) {
        @Suppress("UNUSED_VARIABLE") val jsArray = Uint8Array(content.toTypedArray())
        val file = js("new Blob([jsArray],{type:'application/zip'})") as Blob
        val a = document.createElement("a")
        val url = URL.createObjectURL(file)
        a.setAttribute("href", url)
        a.setAttribute("download", filename)
        document.body!!.appendChild(a)
        (a as HTMLAnchorElement).click()
        document.body!!.removeChild(a)
    }

    suspend fun bumpTestNotification() = postRequest("bump-test-notification")
}
