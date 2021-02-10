package bacs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import util.getLogger
import java.nio.file.Path

class BacsArchiveService(
    private val client: OkHttpClient,
    private val baseUrl: HttpUrl
) {
    companion object {
        const val PENDING_IMPORT = "PENDING_IMPORT"
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun uploadProblem(zip: Path) {
        withContext(Dispatchers.IO) {
            val contentType = "application/zip".toMediaType()
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("_5", "Upload")
                .addFormDataPart("archive", zip.fileName.toString(), zip.toFile().asRequestBody(contentType))
                .addFormDataPart("archiver_format", "")
                .addFormDataPart("archiver_type", "7z")
                .addFormDataPart("response", "html")
                .build()

            val url = baseUrl.newBuilder().addEncodedPathSegment("upload").build()

            val response = client.newCall(Request.Builder().url(url).post(body).build()).execute()

            response.use {
                if (!response.isSuccessful) throw BacsArchiveUploadException("Code ${response.code}, Message ${response.message}")

                val content = response.body!!.string()
                getLogger(javaClass).debug(content)

                if (content.contains("reserved: *$PENDING_IMPORT".toRegex())) return@use

                val flags = "reserved: (\\S+)".toRegex().findAll(content).map { it.groups[1]!!.value }.toList()
                val extra = when {
                    flags.isEmpty() -> "there are no flags"
                    else -> "but found flags ${flags.joinToString(",")}"
                }
                throw BacsArchiveUploadException("Flag $PENDING_IMPORT not found, $extra")
            }
        }
    }
}