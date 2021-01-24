import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.fetch.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.browser.window
import polygon.Package
import polygon.Problem

val endpoint = window.location.origin // only needed until https://github.com/ktorio/ktor/issues/1695 is resolved

val jsonClient = HttpClient {
    install(JsonFeature) { serializer = KotlinxSerializer() }
}

suspend fun getProblems(): List<Problem> {
    return jsonClient.get("$endpoint/problems")
}

suspend fun getPackages(problemId: Int): List<Package> {
    return jsonClient.get("$endpoint/problems/$problemId/packages")
}

suspend fun downloadPackage(problemId: Int, packageId: Int): ByteArray {
    val response = jsonClient.get<HttpResponse>("$endpoint/problems/$problemId/packages/${packageId}")
    val bytes = response.content.toByteArray()
    return bytes
}

fun getDownloadPackageLink(problemId: Int, packageId: Int) = "$endpoint/problems/$problemId/packages/${packageId}"
