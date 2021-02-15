@file:OptIn(ExperimentalTime::class)

import api.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.atomicfu.atomic
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
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

object Api {
    private val client = HttpClient {
        install(JsonFeature) { serializer = KotlinxSerializer() }
        install(WebSockets)
        defaultRequest {
            console.log(window.location)

            url {
                console.log(this)
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
        client.webSocket(path, { url { protocol = wsProtocol } }) {
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
        scope.launch {
            val first = atomic(true)
            while (true) {
                try {
                    console.log("Registering session")
                    getRequest("register-session")
                    console.log("Session registered")
                    console.log("Connecting to WS")
                    connectWS("subscribe") {
                        console.log("Connected to WS")
                        if (!first.value)
                            showToast(
                                Toast(
                                    "Соединение",
                                    "Соединение восстановлено. " +
                                            "С некоторой долей вероятности есть смысл перезагрузить страницу, чтобы подгрузить обновления",
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
                    first.compareAndSet(expect = true, update = false)
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

    suspend fun testAllSolutions(problem: Problem, block: (Map<String, Verdict>) -> Unit) {
        connectWS("problems/${problem.id}/solutions/test-all") {
            val receive = incoming.receive()
            console.log("[INFO] ${receive.frameType}")
            val m = receive as Frame.Text
            val string = m.readText()
            console.log("[INFO] $string")
            val decodeFromString = Json.decodeFromString<Map<String, Verdict>>(string)
            console.log("[INFO] $decodeFromString")
            block(decodeFromString)
        }
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