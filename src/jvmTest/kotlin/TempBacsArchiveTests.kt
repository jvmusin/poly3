import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.StringSpec
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS
import org.jsoup.Jsoup
import sybon.SybonApiFactory
import java.nio.file.Paths

@OptIn(ExperimentalSerializationApi::class)
@Ignored
class TempBacsArchiveTests : StringSpec({
    val api = SybonApiFactory().createArchiveApi()

    "main collection problems should contains some problems with author 'Musin'" {
        val problems = api.getCollection(1).problems
        val myProblems = problems.filter { it.author.contains("Musin", ignoreCase = true) }
        println(myProblems.size)
        myProblems.map { "${it.id} ${it.internalProblemId}" }.forEach(::println)
    }

    "send problem to bacs archive" {
        val zip = Paths.get("4-values-sum-0-low-tl-package174473.zip")
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("_5", "Upload")
            .addFormDataPart(
                "archive",
                zip.fileName.toString(),
                zip.toFile().asRequestBody("application/zip".toMediaType())
            )
            .addFormDataPart("archiver_format", "")
            .addFormDataPart("archiver_type", "7z")
            .addFormDataPart("response", "html")
            .build()

        val loggingInterceptor = HttpLoggingInterceptor(::println).setLevel(HEADERS)
        val authInterceptor = Interceptor() {
            val request = it.request().newBuilder()
                .header("Authorization", "Basic c3lib246d2poJDQyZHMwOQ==")
                .build()
            it.proceed(request)
        }
        val client = OkHttpClient().newBuilder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()
        val request = Request.Builder()
            .url("https://archive.bacs.cs.istu.ru/repository/upload")
            .post(body)
            .build()
        val response = client.newCall(request).execute()

        val document = Jsoup.parse(response.body!!.string())
        val element = document.body()
            .getElementsByTag("table")[0]
            .getElementsByTag("tbody")[0]
            .getElementsByTag("tr")[1]
            .getElementsByTag("td")[2]
            .getElementsByTag("pre")[0]
            .text()
        println(element)
    }
})