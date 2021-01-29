import api.Problem
import api.ProblemInfo
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.browser.window

val endpoint = window.location.origin // only needed until https://github.com/ktorio/ktor/issues/1695 is resolved

val jsonClient = HttpClient {
    install(JsonFeature) { serializer = KotlinxSerializer() }
}

suspend fun getProblems(): List<Problem> = jsonClient.get("$endpoint/problems")

suspend fun getProblemInfo(problemId: Int): ProblemInfo = jsonClient.get("$endpoint/problems/$problemId")

fun downloadPackageLink(problemId: Int) = "$endpoint/problems/$problemId/download"

suspend fun transferToBacsArchive(problemId: Int): Int = jsonClient.get("$endpoint/problems/$problemId/transfer")
