@file:OptIn(ExperimentalTime::class, ExperimentalPathApi::class, ExperimentalCoroutinesApi::class)

import api.*
import bacs.BacsArchiveServiceFactory
import bacs.BacsProblemState
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import polygon.PolygonApiFactory
import polygon.PolygonProblemDownloader
import polygon.PolygonProblemDownloaderException
import polygon.toDto
import sybon.SybonApiFactory
import sybon.SybonArchiveBuilder
import sybon.SybonServiceFactory
import sybon.converter.IRLanguageToCompilerConverter.toSybonCompiler
import sybon.converter.SybonSubmissionResultToSubmissionResultConverter.toSubmissionResult
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

fun Route.routes() {
    val polygonApi = PolygonApiFactory().create()
    val bacsArchiveService = BacsArchiveServiceFactory().create()
    val problemDownloader = PolygonProblemDownloader(polygonApi)
    val sybonArchiveBuilder = SybonArchiveBuilder()
    val sybonService = SybonServiceFactory(SybonApiFactory()).create()

    val wsBySession = ConcurrentHashMap<String, CopyOnWriteArrayList<SendChannel<Frame>>>()
    fun PipelineContext<Unit, ApplicationCall>.sendMessage(
        title: String,
        content: String,
        kind: ToastKind = ToastKind.INFORMATION
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
        val irProblem = try {
            sendMessage(fullName, "Начато скачивание задачи из полигона")
            problemDownloader.download(problemId)
        } catch (e: PolygonProblemDownloaderException) {
            sendMessage(fullName, e.message!!, ToastKind.FAILURE)
            call.respond(
                HttpStatusCode.BadRequest,
                "Не удалось скачать задачу из полигона: ${e.message}"
            )
            return null
        }
        sendMessage(fullName, "Задача скачана, собирается архив")
        val zip = sybonArchiveBuilder.build(irProblem, properties)
        sendMessage(fullName, "Архив собран")
        return zip
    }

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
            val receive = incoming.receive()
            println(receive.frameType)
            if (receive.frameType == FrameType.TEXT) {
                println(receive.readBytes().decodeToString())
            }
        } catch (e: ClosedReceiveChannelException) {
            getLogger(javaClass).info("WS connection closed", e)
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
            val availability = when (bacsArchiveService.getProblemState(name)) {
                BacsProblemState.NOT_FOUND -> NameAvailability.AVAILABLE
                BacsProblemState.IMPORTED, BacsProblemState.PENDING_IMPORT -> NameAvailability.TAKEN
                BacsProblemState.UNKNOWN -> NameAvailability.CHECK_FAILED
            }
            call.respond(HttpStatusCode.OK, availability)
        }
        route("{problem-id}") {
            get {
                val problemId = call.parameters["problem-id"]!!.toInt()
                val problemInfo = polygonApi.getInfo(problemId).result!!
                call.respond(HttpStatusCode.OK, problemInfo.toDto())
            }
            post("download") {
                val fullName = call.parameters["full-name"]!!
                val problemId = call.parameters["problem-id"]!!.toInt()
                val properties = call.receive<AdditionalProblemProperties>()
                val zip = downloadProblemAndBuildArchive(fullName, problemId, properties) ?: return@post
                sendMessage(fullName, "Скачиваем архив", ToastKind.SUCCESS)
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
                val fullName = call.parameters["full-name"].toString()
                val problemId = call.parameters["problem-id"]!!.toInt()
                val properties = call.receive<AdditionalProblemProperties>()
                val zip = downloadProblemAndBuildArchive(fullName, problemId, properties) ?: return@post
                sendMessage(fullName, "Закидываем архив в бакс")
                bacsArchiveService.uploadProblem(zip)
                val status = bacsArchiveService.waitTillProblemIsImported(fullName, 5.minutes)
                if (status.state == BacsProblemState.IMPORTED) {
                    sendMessage(fullName, "Готово! Задача в баксе", ToastKind.SUCCESS)
                    call.respond(HttpStatusCode.OK)
                } else {
                    sendMessage(fullName, "Не получилось закинуть в бакс: $status", ToastKind.FAILURE)
                    call.respond(HttpStatusCode.BadRequest, "Не получилось закинуть в бакс: $status")
                }
            }
            route("solutions") {
                get {
                    val problemId = call.parameters["problem-id"]!!.toInt()
                    val name = call.parameters["name"]!!
                    val irProblem = try {
                        problemDownloader.download(problemId, true)
                    } catch (e: PolygonProblemDownloaderException) {
                        sendMessage(name, e.message!!, ToastKind.FAILURE)
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Не удалось скачать решения из полигона: ${e.message}"
                        )
                        return@get
                    }
                    call.respond(HttpStatusCode.OK, irProblem.solutions.map {
                        Solution(it.name, Language.valueOf(it.language.name), Verdict.valueOf(it.verdict.name))
                    })
                }
                // We need web sockets here to hold a connection longer than 1m on Heroku
                webSocket("test-all") {
                    pingInterval = 10.seconds.toJavaDuration()
                    val problemId = call.parameters["problem-id"]!!.toInt()
                    val problem = problemDownloader.download(problemId, true)
                    val testProblemId = "polybacs-${problem.name}"
                    val sybonProblem = sybonService.getProblemByBacsProblemId(testProblemId, 5.minutes)!!
                    val result = problem.solutions.map { solution ->
                        async {
                            val compiler = solution.language.toSybonCompiler()
                            val result = if (compiler == null) {
                                SubmissionResult(false, null, null)
                            } else {
                                val result = sybonService.submitSolution(
                                    problemId = sybonProblem.id,
                                    solution = solution.content,
                                    compiler = compiler
                                )
                                result.toSubmissionResult()
                            }
                            solution.name to result.overallVerdict
                        }
                    }.awaitAll().toMap()
                    outgoing.send(Frame.Text(Json.encodeToString(result)))
                }
            }
        }
    }
}