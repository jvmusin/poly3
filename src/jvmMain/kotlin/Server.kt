import api.AdditionalProblemProperties
import bacs.BacsArchiveServiceFactory
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
import polygon.getProblem
import polygon.toDto
import sybon.SybonApiFactory
import sybon.SybonArchiveBuildException
import sybon.SybonArchiveBuilder
import util.getLogger
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readBytes
import kotlin.time.ExperimentalTime
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
    val sybonArchiveApi = SybonApiFactory().createArchiveApi()
    val bacsArchiveService = BacsArchiveServiceFactory().create()
    val sybonArchiveBuilder = SybonArchiveBuilder(polygonApi)

    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json()
        }
        install(CORS) {
            method(HttpMethod.Get)
            method(HttpMethod.Post)
            method(HttpMethod.Delete)
            anyHost()
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
        install(WebSockets)

        routing {
            webSocket("ws") {
                getLogger(javaClass).info("CONNECTED")
                repeat(5) {
                    outgoing.send(Frame.Text("Test $it"))
                    delay(5.seconds)
                }
            }
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
                        val zipName = sybonArchiveBuilder.build(problemId, properties)
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(
                                ContentDisposition.Parameters.FileName,
                                zipName
                            ).toString()
                        )
                        val bytes = Paths.get(SybonArchiveBuilder.BUILT_PACKAGES_FOLDER, zipName).readBytes()
                        println(bytes.size / 1024)
                        call.respondFile(Paths.get(SybonArchiveBuilder.BUILT_PACKAGES_FOLDER, zipName).toFile())
                    }
                    post("transfer") {
                        val problemId = call.parameters["problemId"]!!.toInt()
                        val properties = call.receive<AdditionalProblemProperties>()
                        val zipName = sybonArchiveBuilder.build(problemId, properties)
                        bacsArchiveService.uploadProblem(Paths.get(SybonArchiveBuilder.BUILT_PACKAGES_FOLDER, zipName))
                        val problem = polygonApi.getProblem(problemId)
                        val expectedName = "polybacs-${problem.name}"

                        //todo move it out wtf
                        for (i in 0 until 20) {
                            val problems = sybonArchiveApi.getCollection(1).problems
                            val importedProblem = problems.singleOrNull { it.internalProblemId == expectedName }
                            if (importedProblem != null) {
                                getLogger(javaClass).info("Problem $expectedName is imported with id ${importedProblem.id}")
                                call.respond(importedProblem.id)
                                return@post
                            }
                            getLogger(javaClass).info("Problem $expectedName is not in sybon list yet, sleeping for 3 seconds")
                            delay(3.seconds)
                        }
                        call.respond(-1)
                    }
                }
            }
        }
    }.start(wait = true)
}