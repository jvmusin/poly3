import api.AdditionalProblemProperties
import bacs.BacsArchiveServiceFactory
import bacs.BacsProblemStatus
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import org.slf4j.event.Level
import polygon.PolygonApiFactory
import polygon.PolygonProblemDownloader
import polygon.getProblem
import polygon.toDto
import sybon.*
import util.getLogger
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds

val index = """
    <!doctype html>
    <html lang="en">
      <head>
        <!-- Required meta tags -->
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <!-- Bootstrap CSS -->
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta2/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-BmbxuPwQa2lc/FVzBcNJ7UAyJxM6wuqIj61tLrc4wSX0szH/Ev+nYRRuWlolflfl" crossorigin="anonymous">
        
        <!-- Bootstrap Icons -->
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.3.0/font/bootstrap-icons.css">

        <title>Полибакс!!</title>
      </head>
      <body>
        <!-- Main content -->
        <div id="root"></div>
        <!-- JavaScript Bundle with Popper -->
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta2/dist/js/bootstrap.bundle.min.js" integrity="sha384-b5kHyXgcpbZJO/tY9Ul7kGkf1S0CWuKcCD38l8YkeH8z8QjE0GmW1gYU5S9FOnJ0" crossorigin="anonymous"></script>
        <!-- Main script -->
        <script src="/static/output.js"></script>
      </body>
    </html>
""".trimIndent()

@ExperimentalPathApi
@OptIn(ExperimentalTime::class)
fun main() {
    val polygonApi = PolygonApiFactory().create()
    val bacsArchiveService = BacsArchiveServiceFactory().create()
    val sybonService = SybonServiceFactory(SybonApiFactory()).create()

    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json()
        }
        install(Compression) {
            gzip()
        }
        install(CallLogging) {
            level = Level.DEBUG
        }
        install(StatusPages) {
            exception<SybonArchiveBuildException> { cause ->
                call.respond(HttpStatusCode.BadRequest, cause.message ?: "No message provided")
                throw cause
            }
        }
        install(DefaultHeaders)

        routing {
            get {
                call.respondText(index, ContentType.Text.Html)
            }
            static("static") {
                resources()
            }
            route("problems") {
                get {
                    val problems = polygonApi.getProblems().result!!
                    call.respond(HttpStatusCode.OK, problems.map { it.toDto() })
                }
                route("{problemId}") {
                    get {
                        val problemId = call.parameters["problemId"]!!.toInt()
                        val problemInfo = polygonApi.getInfo(problemId).result!!
                        call.respond(HttpStatusCode.OK, problemInfo.toDto())
                    }
                    post("download") {
                        val problemId = call.parameters["problemId"]!!.toInt()
                        val properties = call.receive<AdditionalProblemProperties>()
                        val problem = PolygonProblemDownloader(polygonApi).download(problemId)
                        val zip = SybonArchiveBuilderNew().build(problem, properties)
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(
                                ContentDisposition.Parameters.FileName,
                                zip.name
                            ).toString()
                        )
                        call.respondFile(zip.toFile())
                    }
                    post("transfer") {
                        val problemId = call.parameters["problemId"]!!.toInt()
                        val properties = call.receive<AdditionalProblemProperties>()
                        val irProblem = PolygonProblemDownloader(polygonApi).download(problemId)
                        val zip = SybonArchiveBuilderNew().build(irProblem, properties)
                        bacsArchiveService.uploadProblem(zip)
                        val problem = polygonApi.getProblem(problemId)
                        val fullName = properties.buildFullName(problem.name)
                        repeat(20) {
                            val status = bacsArchiveService.getProblemStatus(fullName)
                            when (status.state) {
                                BacsProblemStatus.State.PENDING_IMPORT -> {
                                    getLogger(javaClass).info("Problem $fullName is still importing. Sleeping for 1s")
                                    delay(1.seconds)
                                }
                                BacsProblemStatus.State.IMPORTED -> {
                                    getLogger(javaClass).info("Problem $fullName imported")
                                    call.respond(HttpStatusCode.OK)
                                    return@post
                                }
                                else -> {
                                    getLogger(javaClass).info("Something went wrong when checking problem $fullName")
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        "Something bad happened to bacs archive while checking problem $fullName"
                                    )
                                    return@post
                                }
                            }
                        }
                        call.respond(HttpStatusCode.BadRequest, "It took too long for the problem $fullName to import")
                    }
                }
            }
        }
    }.start(wait = true)
}