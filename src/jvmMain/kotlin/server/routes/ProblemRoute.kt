package server.routes

import api.AdditionalProblemProperties
import api.ToastKind
import bacs.BacsArchiveBuilder
import bacs.BacsArchiveService
import io.ktor.application.call
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import org.koin.ktor.ext.inject
import polygon.PolygonService
import polygon.api.toDto
import server.MessageSenderFactory
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalPathApi::class, ExperimentalTime::class)
fun Route.problemRoute() {

    val bacsArchiveService: BacsArchiveService by inject()
    val polygonService: PolygonService by inject()
    val messageSenderFactory: MessageSenderFactory by inject()
    val archiveBuilder: BacsArchiveBuilder by inject()

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
        val zip = archiveBuilder.buildArchive(irProblem, properties)
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
        solutionsRoute()
    }
}
