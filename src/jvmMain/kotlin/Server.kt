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
import kotlinx.html.*
import polygon.buildPolygonApi
import sybon.SybonArchiveBuilder

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

fun main() {
    val api = buildPolygonApi()
    val sybonArchiveBuilder = SybonArchiveBuilder(api)

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

        routing {
            get("/") {
                call.respondHtml(HttpStatusCode.OK, HTML::index)
            }
            static("/static") {
                resources()
            }
            route("/problems") {
                get("/") {
                    val problems = api.problem.getProblems().result!!
                    call.respond(HttpStatusCode.OK, problems.map(polygon.Problem::toDto))
                }
                route("/{problemId}") {
                    get("/") {
                        val problemId = call.parameters["problemId"]!!.toInt()
                        val problemInfo = api.problem.getInfo(problemId).result!!
                        call.respond(HttpStatusCode.OK, problemInfo.toDto())
                    }
                    get("/download") {
                        val problemId = call.parameters["problemId"]!!.toInt()
                        val zipPath = sybonArchiveBuilder.build(problemId)
                        call.response.headers.append("Content-Disposition", "filename=\"${zipPath.fileName}\"")
                        call.respondFile(zipPath.toFile())
                    }
                }
            }
        }
    }.start(wait = true)
}