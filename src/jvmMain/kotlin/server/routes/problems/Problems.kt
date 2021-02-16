@file:OptIn(ExperimentalPathApi::class, ExperimentalTime::class)

package server.routes.problems

import api.*
import bacs.BacsArchiveService
import bacs.BacsProblemState
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import polygon.PolygonApi
import polygon.PolygonProblemDownloader
import polygon.PolygonProblemDownloaderException
import polygon.toDto
import server.MessageSender
import server.MessageSenderFactory
import sybon.SybonArchiveBuilder
import sybon.SybonService
import sybon.converter.IRLanguageToCompilerConverter.toSybonCompiler
import sybon.converter.SybonSubmissionResultToSubmissionResultConverter.toSubmissionResult
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds
import kotlin.time.toJavaDuration

fun Route.problems() {
    val polygonApi: PolygonApi by inject()
    val bacsArchiveService: BacsArchiveService by inject()
    val problemDownloader: PolygonProblemDownloader by inject()
    val sybonArchiveBuilder: SybonArchiveBuilder by inject()
    val sybonService: SybonService by inject()
    val messageSenderFactory: MessageSenderFactory by inject()

    suspend fun downloadProblemAndBuildArchive(
        sendMessage: MessageSender,
        fullName: String,
        problemId: Int,
        properties: AdditionalProblemProperties
    ): Path {
        val irProblem = try {
            sendMessage(fullName, "Начато скачивание задачи из полигона")
            problemDownloader.download(problemId)
        } catch (e: PolygonProblemDownloaderException) {
            val msg = "Не удалось скачать задачу из полигона: ${e.message}"
            sendMessage(fullName, msg, ToastKind.FAILURE)
            throw BadRequestException(msg)
        }
        sendMessage(fullName, "Задача скачана, собирается архив")
        val zip = sybonArchiveBuilder.build(irProblem, properties)
        sendMessage(fullName, "Архив собран")
        return zip
    }

    suspend fun transferProblemToBacs(
        sendMessage: MessageSender,
        fullName: String,
        problemId: Int,
        properties: AdditionalProblemProperties
    ) {
        val zip = downloadProblemAndBuildArchive(sendMessage, fullName, problemId, properties)
        sendMessage(fullName, "Закидываем архив в бакс")
        try {
            bacsArchiveService.uploadProblem(zip)
        } catch (e: Exception) {
            val msg = "Задача не закинута в бакс: ${e.message}"
            sendMessage(fullName, msg, ToastKind.FAILURE)
            throw BadRequestException(msg)
        }
        val status = bacsArchiveService.waitTillProblemIsImported(fullName, 5.minutes)
        if (status.state == BacsProblemState.IMPORTED) {
            sendMessage(fullName, "Задача закинута в бакс", ToastKind.SUCCESS)
        } else {
            sendMessage(fullName, "Не получилось закинуть в бакс: $status", ToastKind.FAILURE)
            throw BadRequestException("Не получилось закинуть в бакс: $status")
        }
    }
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
            val zip = downloadProblemAndBuildArchive(messageSenderFactory.createMessageSender(call), fullName, problemId, properties)
            messageSenderFactory.createMessageSender(call)(fullName, "Скачиваем архив", ToastKind.SUCCESS)
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
            transferProblemToBacs(messageSenderFactory.createMessageSender(call), fullName, problemId, properties)
            call.respond(HttpStatusCode.OK)
        }
        route("solutions") {
            get {
                val problemId = call.parameters["problem-id"]!!.toInt()
                val name = call.parameters["name"]!!
                val irProblem = try {
                    problemDownloader.download(problemId, true)
                } catch (e: PolygonProblemDownloaderException) {
                    messageSenderFactory.createMessageSender(call)(name, e.message!!, ToastKind.FAILURE)
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
                val properties = AdditionalProblemProperties(
                    prefix = "polybacs-test-",
                    suffix = null,
                    timeLimitMillis = null,
                    memoryLimitMegabytes = null
                )

                val sendMessage = messageSenderFactory.createMessageSender(call)
                val fullName = properties.buildFullName(problem.name)
                transferProblemToBacs(sendMessage, fullName, problemId, properties)

                sendMessage(fullName, "Ждём, пока задача появится в сайбоне (может занять минуты две)")
                val sybonProblem = sybonService.getProblemByBacsProblemId(fullName, 5.minutes)
                if (sybonProblem == null) {
                    sendMessage(fullName, "За пять минут задача так и не появилась в сайбоне", ToastKind.FAILURE)
                    return@webSocket
                }
                sendMessage(fullName, "Задача появилась в сайбоне, отправляем решения на проверку")
                val result = try {
                    problem.solutions.map { solution ->
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
                } catch (e: Exception) {
                    val msg = "Не удалось проверить решения: ${e.message}"
                    sendMessage(fullName, msg, ToastKind.FAILURE)
                    throw BadRequestException(msg)
                }
                sendMessage(fullName, "Решения проверены", ToastKind.SUCCESS)
                outgoing.send(Frame.Text(Json.encodeToString(result)))
            }
        }
    }
}