package server.routes

import api.AdditionalProblemProperties
import api.Language
import api.Solution
import api.SubmissionResult
import api.ToastKind
import api.Verdict
import bacs.BacsArchiveService
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.websocket.webSocket
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import polygon.PolygonService
import polygon.exception.downloading.ProblemDownloadingException
import server.MessageSenderFactory
import sybon.SybonArchiveService
import sybon.SybonCheckingService
import sybon.SybonSolutionTestingTimeoutException
import sybon.TestProblemArchive
import sybon.converter.IRLanguageToCompilerConverter.toSybonCompiler
import sybon.converter.SybonSubmissionResultToSubmissionResultConverter.toSubmissionResult

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
            polygonService.downloadProblem(problemId)
        } catch (e: ProblemDownloadingException) {
            messageSenderFactory.create(this, fullName)(e.message.orEmpty(), ToastKind.FAILURE)
            call.respond(
                HttpStatusCode.BadRequest,
                "Не удалось скачать решения из полигона: ${e.message}"
            )
            return@get
        }
        call.respond(
            HttpStatusCode.OK,
            irProblem.solutions.map {
                Solution(it.name, Language.valueOf(it.language.name), Verdict.valueOf(it.verdict.name))
            }
        )
    }

    webSocket("prepare") {
        val problemId = call.parameters["problem-id"]!!.toInt()
        val properties = AdditionalProblemProperties(suffix = "-test")
        val fullName = properties.buildFullName(polygonService.downloadProblem(problemId).name) // pass the name

        val sendMessage = messageSenderFactory.create(this, fullName)
        transferProblemToBacs(sendMessage, problemId, properties, false, polygonService, bacsArchiveService)

        sendMessage("Ждём, пока задача появится в сайбоне (может занять минуты две)")
        val sybonProblem = testSybonArchiveService.importProblem(fullName)
        if (sybonProblem == null) {
            sendMessage("Задача так и не появилась в сайбоне", ToastKind.FAILURE)
            return@webSocket
        }
        sendMessage("Задача появилась в сайбоне")
        send(sybonProblem.id.toString())
    }

    webSocket("test") {
        val problemId = call.parameters["problem-id"]!!.toInt()
        val sybonProblemId = (incoming.receive() as Frame.Text).readText().toInt()
        val solutionName = (incoming.receive() as Frame.Text).readText()

        val problem = polygonService.downloadProblem(problemId, true)

        val solution = problem.solutions.single { it.name == solutionName }
        val compiler = solution.language.toSybonCompiler()
        val result =
            if (compiler == null) {
                SubmissionResult(Verdict.NOT_TESTED, message = "Сайбон не знает про ${solution.language.fullName}")
            } else try {
                sybonCheckingService.submitSolutionTimed(sybonProblemId, solution.content, compiler)
                    .toSubmissionResult()
            } catch (e: SybonSolutionTestingTimeoutException) {
                SubmissionResult(Verdict.SERVER_ERROR, message = e.message)
            }

        send(Json.encodeToString(result))
    }
}
