@file:OptIn(ExperimentalTime::class)

package bacs

import api.AdditionalProblemProperties
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import ir.IRProblem
import org.jsoup.Jsoup
import sybon.toZipArchive
import util.RetryPolicy
import util.getLogger
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.fileSize
import kotlin.io.path.readBytes
import kotlin.io.use
import kotlin.random.Random
import kotlin.text.toByteArray
import kotlin.time.*

interface BacsArchiveService {
    suspend fun getProblemState(problemId: String): BacsProblemState
    suspend fun uploadProblem(problem: IRProblem, properties: AdditionalProblemProperties): String
}

class BacsArchiveServiceImpl(
    private val client: HttpClient
) : BacsArchiveService {
    @SuppressWarnings("ALL")
    companion object {
        const val PENDING_IMPORT = "PENDING_IMPORT"

        /**
         * What happened here:
         * I removed two last symbols from the end of the whole multipart form.
         * I made the quotes to always be in multipart's ContentDisposition values.
         * It might be a bug or not, I'll check the sources later to understand,
         * Why they choose to quote names only if needed, since it breaks
         * Compatibility with old servers.
         */

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

        @OptIn(ExperimentalIoApi::class)
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
    }

    @OptIn(ExperimentalPathApi::class)
    private suspend fun uploadProblem(zip: Path) {
        val response = client.post<HttpResponse>("/upload") {
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

        val content = response.receive<String>()
        if (response.status != HttpStatusCode.OK) {
            throw BacsProblemImportException("Code ${response.status}, Message $content")
        }

        getLogger(javaClass).debug(content)

        if (content.contains("reserved: *$PENDING_IMPORT".toRegex())) return

        val flags = "reserved: (\\S+)".toRegex().findAll(content).map { it.groups[1]!!.value }.toList()
        val extra = when {
            flags.isEmpty() -> "there are no flags"
            else -> "but found flags ${flags.joinToString(",")}"
        }
        throw BacsProblemImportException("Flag $PENDING_IMPORT not found, $extra")
    }

    private suspend fun getProblemStatus(problemId: String): BacsProblemStatus {
        val content = client.post<String>("/status") {
            body = MultiPartFormDataContent(
                formData {
                    append("response", "html")
                    append("ids", problemId)
                    append("_4", "Get status")
                }
            )
        }

        getLogger(javaClass).debug(content)
        throw Exception(content)

        val row = Jsoup.parse(content).body()
            .getElementsByTag("table")[0]
            .getElementsByTag("tbody")[0]
            .getElementsByTag("tr")[1]
            .getElementsByTag("td")
            .map { it.text().trim() }

        if (row.size == 2) {
            return BacsProblemStatus(row[1], emptyList(), "")
        }

        val flagRegex = "flag\\{(?<name>.*?):(?<value>.*?)}".toRegex()

        val name = row[1]
        val revision = row[3]
        val flagsRaw = row[2].replace("\\s".toRegex(), "")
        val flags = flagRegex.findAll(flagsRaw)
            .map { "${it.groups["name"]!!.value}: ${it.groups["value"]!!.value}" }
            .toList()
        return BacsProblemStatus(name, flags, revision)
    }

    override suspend fun getProblemState(problemId: String): BacsProblemState {
        return try {
            getProblemStatus(problemId).state
        } catch (e: Exception) {
            throw e
            getLogger(javaClass).trace("Failed to get problem status", e)
            BacsProblemState.UNKNOWN
        }
    }

    private suspend fun waitTillProblemIsImported(
        problemId: String,
        retryPolicy: RetryPolicy = RetryPolicy()
    ): BacsProblemState {
        return retryPolicy.evalWhileFails({ it != null && it != BacsProblemState.PENDING_IMPORT }) {
            getProblemStatus(problemId).state
        }!!
    }

    override suspend fun uploadProblem(problem: IRProblem, properties: AdditionalProblemProperties): String {
        val zip = problem.toZipArchive(properties)
        uploadProblem(zip)
        val fullName = properties.buildFullName(problem.name)
        val state = waitTillProblemIsImported(fullName)
        if (state != BacsProblemState.IMPORTED)
            throw BacsProblemImportException("Задача $fullName не импортирована, статус $state")
        return fullName
    }
}