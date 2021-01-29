package bacs

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import util.getLogger
import java.nio.file.Path

suspend fun BacsArchiveApi.uploadProblem(zip: Path): String {
    val contentType = "application/zip".toMediaType()
    val body = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("_5", "Upload")
        .addFormDataPart("archive", zip.fileName.toString(), zip.toFile().asRequestBody(contentType))
        .addFormDataPart("archiver_format", "")
        .addFormDataPart("archiver_type", "7z")
        .addFormDataPart("response", "html")
        .build()
    val html = uploadProblem(body)
    getLogger(javaClass).debug("Problem is uploaded. Response html:")
    getLogger(javaClass).info(html)
    return html
}