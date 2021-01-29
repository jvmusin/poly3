import bacs.BacsArchiveApiFactory
import bacs.uploadProblem
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.delay
import kotlinx.html.*
import org.slf4j.event.Level
import polygon.PolygonApiFactory
import polygon.getProblem
import polygon.toDto
import sybon.SybonApiFactory
import sybon.SybonArchiveBuildException
import sybon.SybonArchiveBuilder
import sybon.SybonArchiveProperties
import util.getLogger
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

fun HTML.index() {
    head {
        title("Полибакс!!")
    }
    body {
        div {
            id = "root"
        }
        script(src = "/static/output.js") {}
    }
}

@OptIn(ExperimentalTime::class)
fun main() {
    val polygonApi = PolygonApiFactory().create()
    val sybonArchiveApi = SybonApiFactory().createArchiveApi()
    val bacsArchiveApi = BacsArchiveApiFactory().create()
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

        routing {
            get("/") {
                call.respondHtml(HttpStatusCode.OK, HTML::index)
            }
            static("/static") {
                resources()
            }
            route("/problems") {
                get("/") {
                    val problems = polygonApi.getProblems().result!!
                    call.respond(HttpStatusCode.OK, problems.map { it.toDto() })
                }
                route("/{problemId}") {
                    get("/") {
                        val problemId = call.parameters["problemId"]!!.toInt()
                        val problemInfo = polygonApi.getInfo(problemId).result!!
                        call.respond(HttpStatusCode.OK, problemInfo.toDto())
                    }
                    get("/download") {
                        val problemId = call.parameters["problemId"]!!.toInt()
                        val zipPath = sybonArchiveBuilder.build(problemId, SybonArchiveProperties("polybacs-"))
                        call.response.headers.append("Content-Disposition", "filename=\"${zipPath.fileName}\"")
                        call.respondFile(zipPath.toFile())
                    }
                    get("/transfer") {
                        val problemId = call.parameters["problemId"]!!.toInt()
                        val zipPath = sybonArchiveBuilder.build(problemId, SybonArchiveProperties("polybacs-"))
                        bacsArchiveApi.uploadProblem(zipPath)
                        val problem = polygonApi.getProblem(problemId)
                        val expectedName = "polybacs-${problem.name}"
                        for (i in 0 until 20) {
                            val problems = sybonArchiveApi.getCollection(1).problems
                            val importedProblem = problems.singleOrNull { it.internalProblemId == expectedName }
                            if (importedProblem != null) {
                                getLogger(javaClass).info("Problem $expectedName is imported with id ${importedProblem.id}")
                                call.respond(importedProblem.id)
                                return@get
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