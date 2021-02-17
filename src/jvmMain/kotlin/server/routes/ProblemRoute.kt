package server.routes

import api.AdditionalProblemProperties
import api.ToastKind
import bacs.BacsArchiveService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.koin.ktor.ext.inject
import polygon.PolygonService
import polygon.toDto
import server.MessageSenderFactory
import sybon.toZipArchive
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalPathApi::class, ExperimentalTime::class)
fun Route.problem() {

    val bacsArchiveService: BacsArchiveService by inject()
    val polygonService: PolygonService by inject()
    val messageSenderFactory: MessageSenderFactory by inject()

    get {
        val problemId = call.parameters["problem-id"]!!.toInt()
        val problemInfo = polygonService.getProblemInfo(problemId)
        call.respond(HttpStatusCode.OK, problemInfo.toDto())
    }
    post("download") {
        val fullName = call.parameters["full-name"]!!
        val problemId = call.parameters["problem-id"]!!.toInt()
        val properties = call.receive<AdditionalProblemProperties>()
        val sendMessage = messageSenderFactory.create(this, fullName)
        val irProblem = downloadProblem(sendMessage, problemId, polygonService)
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
        transferProblemToBacs(
            messageSenderFactory.create(this, fullName),
            problemId,
            properties,
            true,
            polygonService,
            bacsArchiveService
        )
        call.respond(HttpStatusCode.OK)
    }
    route("solutions") {
        solutions()
    }
}