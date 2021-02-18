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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import polygon.PolygonProblemDownloadException
import polygon.PolygonService
import server.MessageSenderFactory
import sybon.SybonArchiveService
import sybon.SybonCheckingService
import sybon.SybonSolutionTestingException
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

    webSocket("prepare") {
        val problemId = call.parameters["problem-id"]!!.toInt()
        val problem = polygonService.downloadProblem(problemId, true)
        val properties = AdditionalProblemProperties(suffix = "-test")

        val fullName = properties.buildFullName(problem.name)
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
                SubmissionResult(Verdict.COMPILATION_ERROR)   // TODO provide message
            } else try {
                sybonCheckingService.submitSolution(sybonProblemId, solution.content, compiler).toSubmissionResult()
            } catch (e: SybonSolutionTestingException) {
                throw SybonSolutionTestingException("Solution ${solution.name} was not tested: ${e.message}", e)
            }

        send(Json.encodeToString(result))
    }
}