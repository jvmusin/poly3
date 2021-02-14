import api.AdditionalProblemProperties
import api.Problem
import api.ProblemInfo
import api.Toast
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
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob

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
            parameter("fullName", fullName)
        }.readBytes()
        downloadZip(bytes, "$fullName.zip")
    }

    suspend fun transferToBacsArchive(problem: Problem, props: AdditionalProblemProperties) {
        val fullName = props.buildFullName(problem.name)
        postRequest("problems/${problem.id}/transfer") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            body = props
            parameter("fullName", fullName)
        }
    }

    suspend fun registerNotifications() {
        getRequest("register-session")
        scope.launch {
            try {
                console.log("Connecting to WS")
                connectWS("subscribe") {
                    incoming.consumeAsFlow()
                        .takeWhile { it is Frame.Text }
                        .map { it as Frame.Text }
                        .map { it.readText() }
                        .map { Json.decodeFromString<Toast>(it) }
                        .collect { showToast(it) }
                }
            } finally {
                console.log("Disconnected from WS")
            }
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