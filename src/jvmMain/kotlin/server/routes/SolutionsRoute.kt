package server.routes

import api.*
import bacs.BacsArchiveService
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import polygon.PolygonProblemDownloadException
import polygon.PolygonService
import server.MessageSenderFactory
import sybon.SybonArchiveService
import sybon.SybonCheckingService
import sybon.TestProblemArchive
import sybon.converter.IRLanguageToCompilerConverter.toSybonCompiler
import sybon.converter.SybonSubmissionResultToSubmissionResultConverter.toSubmissionResult
import kotlin.time.ExperimentalTime
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@OptIn(ExperimentalTime::class)
fun Route.solutions() {

    val bacsArchiveService: BacsArchiveService by inject()
    val polygonService: PolygonService by inject()
    val testSybonArchiveService: SybonArchiveService by inject(TestProblemArchive)
    val sybonCheckingService: SybonCheckingService by inject()
    val messageSenderFactory: MessageSenderFactory by inject()

    get {
        val problemId = call.parameters["problem-id"]!!.toInt()
        val fullName = call.parameters["name"]!!
        val irProblem = try {
            polygonService.downloadProblem(problemId, true)
        } catch (e: PolygonProblemDownloadException) {
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
        val problem = polygonService.downloadProblem(problemId, true)
        val properties = AdditionalProblemProperties()

        val fullName = properties.buildFullName(problem.name)
        val sendMessage = messageSenderFactory.create(this, fullName)
        transferProblemToBacs(sendMessage, problemId, properties, false, polygonService, bacsArchiveService)

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