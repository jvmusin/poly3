import io.kotest.core.spec.style.StringSpec
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.Headers
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.jvm.javaio.*
import okhttp3.*
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.fileSize
import kotlin.io.path.readBytes
import kotlin.random.Random

@OptIn(ExperimentalPathApi::class)
class ComparingClients : StringSpec({
    "ktor" {

        val zip = Paths.get("4-values-sum-0-low-tl-package174473.zip")

        val url = "$BASE_URL/upload"
        val client = HttpClient(CIO) {
            Auth {
                basic {
                    username = AUTH_USERNAME
                    password = AUTH_PASSWORD
                }
            }
            io.ktor.util.url { takeFrom(BASE_URL) }
        }

        val response = client.post<HttpResponse>(url) {
            body = MultiPartFormDataContent(
                formData {
                    append("_5", "Upload")
                    append("archiver_format", "")
                    append("archiver_type", "7z")
                    append("response", "html")
                    append("archive", zip.fileName.toString(), ContentType.Application.Zip, zip.fileSize()) {
                        writeFully(zip.readBytes())
                    }
                }
            )
        }
        println(response.status)

        println(response.receive<String>())
    }
}) {
    companion object {
        const val AUTH_USERNAME = "sybon"
        const val AUTH_PASSWORD = "wjh\$42ds09"
//        const val BASE_URL = "http://localhost:8080"
        const val BASE_URL = "https://archive.bacs.cs.istu.ru/repository"
    }
}

/**
 * Escape string using double quotes
 */
fun String.quote(): String = buildString {
    append("\"")
    for (ch in this@quote) {
        when (ch) {
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '\"' -> append("\\\"")
            else -> append(ch)
        }
    }
    append("\"")
}

/**
 * Multipart form item. Use it to build form in client.
 *
 * @param key multipart name
 * @param value content, could be [String], [Number] or [Input]
 * @param headers part headers, note that some servers may fail if an unknown header provided
 */
data class FormPart<T : Any>(val key: String, val value: T, val headers: Headers = Headers.Empty)

/**
 * Build multipart form from [values].
 */
fun formData(vararg values: FormPart<*>): List<PartData> {
    val result = mutableListOf<PartData>()

    values.forEach { (key, value, headers) ->
        val partHeaders = HeadersBuilder().apply {
            append(HttpHeaders.ContentDisposition, "form-data; name=${key.quote()}")
            appendAll(headers)
        }
        val part = when (value) {
            is String -> PartData.FormItem(value, {}, partHeaders.build())
            is Number -> PartData.FormItem(value.toString(), {}, partHeaders.build())
            is ByteArray -> {
                partHeaders.append(HttpHeaders.ContentLength, value.size.toString())
                PartData.BinaryItem({ ByteReadPacket(value) }, {}, partHeaders.build())
            }
            is ByteReadPacket -> {
                partHeaders.append(HttpHeaders.ContentLength, value.remaining.toString())
                PartData.BinaryItem({ value.copy() }, { value.close() }, partHeaders.build())
            }
            is InputProvider -> {
                val size = value.size
                if (size != null) {
                    partHeaders.append(HttpHeaders.ContentLength, size.toString())
                }
                PartData.BinaryItem(value.block, {}, partHeaders.build())
            }
            is Input -> error("Can't use [Input] as part of form: $value. Consider using [InputProvider] instead.")
            else -> error("Unknown form content type: $value")
        }

        result += part
    }

    return result
}

/**
 * Build multipart form using [block] function.
 */
private fun formData(block: FormBuilder.() -> Unit): List<PartData> =
    formData(*FormBuilder().apply(block).build().toTypedArray())

/**
 * Form builder type used in [formData] builder function.
 */
private class FormBuilder {
    private val parts = mutableListOf<FormPart<*>>()

    /**
     * Append a pair [key]:[value] with optional [headers].
     */
    fun append(key: String, value: String, headers: Headers = Headers.Empty) {
        parts += FormPart(key, value, headers)
    }

    fun append(
        key: String,
        filename: String,
        contentType: ContentType? = null,
        size: Long? = null,
        bodyBuilder: BytePacketBuilder.() -> Unit
    ) {
        val headersBuilder = HeadersBuilder()
        headersBuilder[HttpHeaders.ContentDisposition] = "filename=${filename.quote()}"
        contentType?.run { headersBuilder[HttpHeaders.ContentType] = this.toString() }
        val headers = headersBuilder.build()

        /**
         * Append a form [part].
         */
        this.parts += FormPart(key, InputProvider(size) { buildPacket { this.bodyBuilder() } }, headers)
    }

    fun build(): List<FormPart<*>> = parts
}

/**
 * Reusable [Input] form entry.
 *
 * @property size estimate for data produced by the block or `null` if no size estimation known
 * @param block: content generator
 */
private class InputProvider(val size: Long? = null, val block: () -> Input)

private val RN_BYTES = "\r\n".toByteArray()

/**
 * [OutgoingContent] for multipart/form-data formatted request.
 *
 * @param parts: form part data
 */
private class MultiPartFormDataContent(
    parts: List<PartData>
) : OutgoingContent.WriteChannelContent() {
    private val boundary: String = generateBoundary()
    private val BOUNDARY_BYTES = "--$boundary\r\n".toByteArray()
    private val LAST_BOUNDARY_BYTES = "--$boundary--\r\n".toByteArray()

    private val BODY_OVERHEAD_SIZE = LAST_BOUNDARY_BYTES.size
    private val PART_OVERHEAD_SIZE = RN_BYTES.size * 2 + BOUNDARY_BYTES.size

    private val rawParts: List<PreparedPart> = parts.map { part ->
        val headersBuilder = BytePacketBuilder()
        for ((key, values) in part.headers.entries()) {
            headersBuilder.writeText("$key: ${values.joinToString("; ")}")
            headersBuilder.writeFully(RN_BYTES)
        }

        val bodySize = part.headers[HttpHeaders.ContentLength]?.toLong()
        when (part) {
            is PartData.FileItem -> {
                val headers = headersBuilder.build().readBytes()
                val size = bodySize?.plus(PART_OVERHEAD_SIZE)?.plus(headers.size)
                PreparedPart(headers, part.provider, size)
            }
            is PartData.BinaryItem -> {
                val headers = headersBuilder.build().readBytes()
                val size = bodySize?.plus(PART_OVERHEAD_SIZE)?.plus(headers.size)
                PreparedPart(headers, part.provider, size)
            }
            is PartData.FormItem -> {
                val bytes = buildPacket { writeText(part.value) }.readBytes()
                val provider = { buildPacket { writeFully(bytes) } }
                if (bodySize == null) {
                    headersBuilder.writeText("${HttpHeaders.ContentLength}: ${bytes.size}")
                    headersBuilder.writeFully(RN_BYTES)
                }

                val headers = headersBuilder.build().readBytes()
                val size = bytes.size + PART_OVERHEAD_SIZE + headers.size
                PreparedPart(headers, provider, size.toLong())
            }
        }
    }

    override val contentLength: Long?

    override val contentType: ContentType = ContentType.MultiPart.FormData.withParameter("boundary", boundary)

    init {
        var rawLength: Long? = 0
        for (part in rawParts) {
            val size = part.size
            if (size == null) {
                rawLength = null
                break
            }

            rawLength = rawLength?.plus(size)
        }

        if (rawLength != null) {
            rawLength += BODY_OVERHEAD_SIZE
        }

        contentLength = rawLength
    }

    override suspend fun writeTo(channel: ByteWriteChannel) {
        try {
            for (part in rawParts) {
                channel.writeFully(BOUNDARY_BYTES)
                channel.writeFully(part.headers)
                channel.writeFully(RN_BYTES)

                part.provider().use { input ->
                    input.copyTo(channel)
                }

                channel.writeFully(RN_BYTES)
            }

            channel.writeFully(LAST_BOUNDARY_BYTES)
        } catch (cause: Throwable) {
            channel.close(cause)
        } finally {
            channel.close()
        }
    }
}

private fun generateBoundary(): String = buildString {
    repeat(32) {
        append(Random.nextInt().toString(16))
    }
}.take(70)

private class PreparedPart(
    val headers: ByteArray,
    val provider: () -> Input,
    val size: Long?
)

private suspend fun Input.copyTo(channel: ByteWriteChannel) {
    if (this is ByteReadPacket) {
        channel.writePacket(this)
        return
    }

    while (!this@copyTo.endOfInput) {
        channel.write { freeSpace, startOffset, endExclusive ->
            this@copyTo.readAvailable(freeSpace, startOffset, endExclusive - startOffset).toInt()
        }
    }
}
