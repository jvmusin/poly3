@file:OptIn(ExperimentalTime::class, ExperimentalPathApi::class, ExperimentalCoroutinesApi::class)

import api.AdditionalProblemProperties
import api.BacsNameAvailability.*
import api.Toast
import api.ToastKind
import api.ToastKind.*
import bacs.BacsArchiveServiceFactory
import bacs.BacsProblemState.*
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
import io.ktor.sessions.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import polygon.PolygonApiFactory
import polygon.PolygonProblemDownloader
import polygon.PolygonProblemDownloaderException
import polygon.toDto
import sybon.SybonArchiveBuilder
import util.getLogger
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds
import kotlin.time.toJavaDuration

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

data class Session(val str: String)

fun main() {
    val polygonApi = PolygonApiFactory().create()
    val bacsArchiveService = BacsArchiveServiceFactory().create()
    val problemDownloader = PolygonProblemDownloader(polygonApi)
    val sybonArchiveBuilder = SybonArchiveBuilder()

    val wsBySession = ConcurrentHashMap<String, CopyOnWriteArrayList<SendChannel<Frame>>>()
    fun PipelineContext<Unit, ApplicationCall>.sendMessage(
        title: String,
        content: String,
        kind: ToastKind = INFORMATION
    ) {
        val list = wsBySession[call.sessions.get<Session>()!!.str]!!
        list.removeIf { it.isClosedForSend }
        list.map { out ->
            launch {
                try {
                    out.send(Frame.Text(Json.encodeToString(Toast(title, content, kind))))
                } catch (e: Exception) {
                    getLogger(javaClass).warn(e.message)
                }
            }
        }
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.downloadProblemAndBuildArchive(
        fullName: String,
        problemId: Int,
        properties: AdditionalProblemProperties
    ): Path? {
        val irProblem = run {
            try {
                sendMessage(fullName, "Начато скачивание задачи из полигона")
                return@run problemDownloader.download(problemId)
            } catch (e: PolygonProblemDownloaderException) {
                sendMessage(fullName, e.message!!, FAILURE)
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Не удалось скачать задачу из полигона: ${e.message}"
                )
                return null
            }
        }
        sendMessage(fullName, "Задача скачана, собирается архив")
        val zip = sybonArchiveBuilder.build(irProblem, properties)
        sendMessage(fullName, "Архив собран")
        return zip
    }

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
            exception<PolygonProblemDownloaderException> { cause ->
                call.respond(HttpStatusCode.BadRequest, cause.message ?: "No message provided")
                throw cause
            }
        }
        install(DefaultHeaders)
        install(WebSockets)
        install(Sessions) {
            cookie<Session>("SESSION")
        }

        routing {
            static("static") {
                resources()
            }
            get("register-session") {
                getLogger(javaClass).info("Registering session")
                call.sessions.getOrSet { Session(UUID.randomUUID().toString()) }
                getLogger(javaClass).info("Registered session")
                call.respond(HttpStatusCode.OK)
            }
            webSocket("subscribe") {
                pingInterval = 10.seconds.toJavaDuration()
                getLogger(javaClass).info("Subscribing WS")
                wsBySession.computeIfAbsent(call.sessions.get<Session>()!!.str) { CopyOnWriteArrayList() }
                    .add(outgoing)
                getLogger(javaClass).info("Subscribed WS")
                try {
                    incoming.receive()
                } catch (e: ClosedReceiveChannelException) {
                    getLogger(javaClass).info("WS connection closed")
                }
            }
            post("bump-test-notification") {
                sendMessage("Привет!", "Хорошо сейчас на улице, выйди прогуляйся")
                call.respond(HttpStatusCode.OK)
            }
            get {
                call.respondText(index, ContentType.Text.Html)
            }
            route("problems") {
                get {
                    val problems = polygonApi.getProblems().result!!
                    call.respond(HttpStatusCode.OK, problems.map { it.toDto() })
                }
                get("get-name-availability") {
                    val name = call.parameters["name"]!!
                    val state = bacsArchiveService.getProblemStatus(name).state
                    val availability = when (state) {
                        NOT_FOUND -> AVAILABLE
                        IMPORTED, PENDING_IMPORT -> TAKEN
                        UNKNOWN -> CHECK_FAILED
                    }
                    call.respond(HttpStatusCode.OK, availability)
                    bacsArchiveService.getProblemStatus(name)
                }
                route("{problemId}") {
                    get {
                        val problemId = call.parameters["problemId"]!!.toInt()
                        val problemInfo = polygonApi.getInfo(problemId).result!!
                        call.respond(HttpStatusCode.OK, problemInfo.toDto())
                    }
                    post("download") {
                        val fullName = call.parameters["fullName"]!!
                        val problemId = call.parameters["problemId"]!!.toInt()
                        val properties = call.receive<AdditionalProblemProperties>()
                        val zip = downloadProblemAndBuildArchive(fullName, problemId, properties) ?: return@post
                        sendMessage(fullName, "Скачиваем архив", SUCCESS)
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
                        val fullName = call.parameters["fullName"].toString()
                        val problemId = call.parameters["problemId"]!!.toInt()
                        val properties = call.receive<AdditionalProblemProperties>()
                        val zip = downloadProblemAndBuildArchive(fullName, problemId, properties) ?: return@post
                        sendMessage(fullName, "Закидываем архив в бакс")
                        bacsArchiveService.uploadProblem(zip)
                        val status = bacsArchiveService.waitTillProblemIsImported(fullName, 5.minutes)
                        if (status.state == IMPORTED) {
                            sendMessage(fullName, "Готово! Задача в баксе", SUCCESS)
                            call.respond(HttpStatusCode.OK)
                        } else {
                            sendMessage(fullName, "Не получилось закинуть в бакс: $status", FAILURE)
                            call.respond(HttpStatusCode.BadRequest, "Не получилось закинуть в бакс: $status")
                        }
                    }
//                    post("test") {
//                        val problemId = call.parameters["problemId"]!!.toInt()
//                        val properties = call.receive<AdditionalProblemProperties>()
//                        val irProblem = transfer(problemId, properties, false, call)
//                        val bacsProblemName = properties.buildFullName(irProblem.name)
//                        val sybonProblemName = sybonService.getProblemByBacsProblemId(bacsProblemName, 5.minutes)
//                        val solutions = irProblem.solutions
//                        //submit all the solutions and wait for verdicts
//                        //send info about all solutions abd their verdicts, say if everything is ok
//                    }
                }
            }
        }
    }.start(wait = true)
}