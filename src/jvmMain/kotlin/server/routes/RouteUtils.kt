package server.routes

import api.AdditionalProblemProperties
import api.StatementFormat
import api.ToastKind
import bacs.BacsArchiveService
import io.ktor.features.BadRequestException
import ir.IRProblem
import polygon.PolygonService
import polygon.exception.downloading.ProblemDownloadingException
import server.MessageSender

suspend fun downloadProblem(sendMessage: MessageSender, problemId: Int, polygonService: PolygonService, statementFormat: StatementFormat = StatementFormat.PDF): IRProblem {
    try {
        sendMessage("Выкачиваем задачу из полигона")
        return polygonService.downloadProblem(problemId, true, statementFormat)
    } catch (e: ProblemDownloadingException) {
        val msg = "Не удалось выкачать задачу из полигона: ${e.message}"
        sendMessage(msg, ToastKind.FAILURE)
        throw BadRequestException(msg, e)
    }
}

suspend fun transferProblemToBacs(
    sendMessage: MessageSender,
    problemId: Int,
    properties: AdditionalProblemProperties,
    isFinalStep: Boolean,
    polygonService: PolygonService,
    bacsArchiveService: BacsArchiveService
) {
    val irProblem = downloadProblem(sendMessage, problemId, polygonService, properties.statementFormat)
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
