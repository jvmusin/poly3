@file:OptIn(ExperimentalPathApi::class, ExperimentalTime::class)

package server.routes.problems

import api.*
import bacs.BacsArchiveService
import bacs.BacsProblemState
import io.ktor.application.*
import io.ktor.client.utils.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import ir.IRProblem
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
import sybon.*
import sybon.converter.IRLanguageToCompilerConverter.toSybonCompiler
import sybon.converter.SybonSubmissionResultToSubmissionResultConverter.toSubmissionResult
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name
import kotlin.time.ExperimentalTime
import kotlin.time.seconds
import kotlin.time.toJavaDuration

fun Route.problems() {
    val polygonApi: PolygonApi by inject()
    val bacsArchiveService: BacsArchiveService by inject()
    val problemDownloader: PolygonProblemDownloader by inject()
    val testSybonArchiveService: SybonArchiveService by inject(TestProblemArchive)
    val sybonCheckingService: SybonCheckingService by inject()
    val messageSenderFactory: MessageSenderFactory by inject()

    suspend fun downloadProblem(sendMessage: MessageSender, problemId: Int): IRProblem {
        return try {
            sendMessage("Начато выкачивание задачи из полигона")
            problemDownloader.download(problemId)
        } catch (e: PolygonProblemDownloaderException) {
            val msg = "Не удалось выкачать задачу из полигона: ${e.message}"
            sendMessage(msg, ToastKind.FAILURE)
            throw BadRequestException(msg, e)
        }
    }

    suspend fun transferProblemToBacs(
        sendMessage: MessageSender,
        problemId: Int,
        properties: AdditionalProblemProperties,
        isFinalStep: Boolean
    ) {
        val irProblem = downloadProblem(sendMessage, problemId)
        sendMessage("Задача выкачана из полигона, закидываем в бакс")
        try {
            bacsArchiveService.uploadProblem(irProblem, properties)
        } catch (e: Exception) {
            val msg = "Не удалось закинуть задачу в бакс: ${e.message}"
            sendMessage(msg, ToastKind.FAILURE)
            throw BadRequestException(msg, e)
        }
        sendMessage("Задача закинута в бакс", if (isFinalStep) ToastKind.SUCCESS else ToastKind.INFORMATION)
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
            val sendMessage = messageSenderFactory.create(this, fullName)
            val irProblem = downloadProblem(sendMessage, problemId)
            val zip = irProblem.toZipArchive(properties)
            sendMessage("Задача выкачана из полигона, скачиваем архив", ToastKind.SUCCESS)
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
            transferProblemToBacs(messageSenderFactory.create(this, fullName), problemId, properties, true)
            call.respond(HttpStatusCode.OK)
        }
        route("solutions") {
            get {
                val problemId = call.parameters["problem-id"]!!.toInt()
                val fullName = call.parameters["name"]!!
                val irProblem = try {
                    problemDownloader.download(problemId, true)
                } catch (e: PolygonProblemDownloaderException) {
                    messageSenderFactory.create(this, fullName)(e.message.orEmpty(), ToastKind.FAILURE)
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
                val properties = AdditionalProblemProperties()

                val fullName = properties.buildFullName(problem.name)
                val sendMessage = messageSenderFactory.create(this, fullName)
                transferProblemToBacs(sendMessage, problemId, properties, false)

                sendMessage("Ждём, пока задача появится в сайбоне (может занять минуты две)")
                val sybonProblem = testSybonArchiveService.importProblem(fullName)
                if (sybonProblem == null) {
                    sendMessage("Задача так и не появилась в сайбоне", ToastKind.FAILURE)
                    return@webSocket
                }
                sendMessage("Задача появилась в сайбоне, отправляем решения на проверку")
                val result = try {
                    problem.solutions.map { solution ->
                        async {
                            val compiler = solution.language.toSybonCompiler()
                            val result = if (compiler == null) {
                                SubmissionResult(false, null, null)
                            } else {
                                val result = sybonCheckingService.submitSolution(
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
                    sendMessage(msg, ToastKind.FAILURE)
                    throw BadRequestException(msg)
                }
                sendMessage("Решения проверены", ToastKind.SUCCESS)
                outgoing.send(Frame.Text(Json.encodeToString(result)))
            }
        }
    }
}