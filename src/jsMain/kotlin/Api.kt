import api.AdditionalProblemProperties
import api.Problem
import api.ProblemInfo
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob

val endpoint = window.location.origin // only needed until https://github.com/ktorio/ktor/issues/1695 is resolved

val client = HttpClient {
    install(JsonFeature) { serializer = KotlinxSerializer() }
    install(WebSockets)
}

suspend fun getProblems(): List<Problem> = client.get("$endpoint/problems")

suspend fun getProblemInfo(problemId: Int): ProblemInfo = client.get("$endpoint/problems/$problemId")

suspend fun downloadPackage(problem: Problem, props: AdditionalProblemProperties) {
    val bytes = client.post<HttpResponse>("$endpoint/problems/${problem.id}/download") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        body = props
    }.readBytes()
    val fullName = "${props.prefix.orEmpty()}${problem.name}${props.suffix.orEmpty()}"
    downloadZip(bytes, "$fullName.zip")
}

suspend fun transferToBacsArchive(problemId: Int, props: AdditionalProblemProperties) {
    client.post<Unit>("$endpoint/problems/$problemId/transfer") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        body = props
    }
}

// https://stackoverflow.com/a/30832210/4296219
fun downloadZip(content: ByteArray, filename: String) {
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