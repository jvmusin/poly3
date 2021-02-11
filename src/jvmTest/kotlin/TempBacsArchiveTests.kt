//import bacs.BacsArchiveService
//import io.kotest.core.annotation.Ignored
//import io.kotest.core.spec.style.StringSpec
//import io.ktor.client.*
//import io.ktor.client.request.*
//import io.ktor.client.request.forms.*
//import io.ktor.client.statement.*
//import io.ktor.http.*
//import io.ktor.http.content.*
//import io.ktor.util.*
//import io.ktor.utils.io.*
//import io.ktor.utils.io.core.*
//import okhttp3.*
//import okhttp3.HttpUrl.Companion.toHttpUrl
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.RequestBody.Companion.asRequestBody
//import okhttp3.logging.HttpLoggingInterceptor
//import okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS
//import org.jsoup.Jsoup
//import sybon.SybonApiFactory
//import util.getLogger
//import java.nio.file.Paths
//import kotlin.io.path.ExperimentalPathApi
//import kotlin.io.path.fileSize
//import kotlin.io.path.readBytes
//
////@OptIn(ExperimentalSerializationApi::class)
//@KtorExperimentalAPI
//@OptIn(ExperimentalPathApi::class)
//@Ignored
//class TempBacsArchiveTests : StringSpec({
//    val api = SybonApiFactory().createArchiveApi()
//
//    "main collection problems should contains some problems with author 'Musin'" {
//        val problems = api.getCollection(1).problems
//        val myProblems = problems.filter { it.author.contains("Musin", ignoreCase = true) }
//        println(myProblems.size)
//        myProblems.map { "${it.id} ${it.internalProblemId}" }.forEach(::println)
//    }
//
//    "send problem to bacs archive" {
//        val zip = Paths.get("4-values-sum-0-low-tl-package174473.zip")
//        val body = MultipartBody.Builder()
//            .setType(MultipartBody.FORM)
//            .addFormDataPart("_5", "Upload")
//            .addFormDataPart(
//                "archive",
//                zip.fileName.toString(),
//                zip.toFile().asRequestBody("application/zip".toMediaType())
//            )
//            .addFormDataPart("archiver_format", "")
//            .addFormDataPart("archiver_type", "7z")
//            .addFormDataPart("response", "html")
//            .build()
//
//        val loggingInterceptor = HttpLoggingInterceptor(::println).setLevel(HEADERS)
//        val authInterceptor = Interceptor() {
//            val request = it.request().newBuilder()
//                .header("Authorization", "Basic c3lib246d2poJDQyZHMwOQ==")
//                .build()
//            it.proceed(request)
//        }
//        val client = OkHttpClient().newBuilder()
//            .addInterceptor(loggingInterceptor)
//            .addInterceptor(authInterceptor)
//            .build()
//        val request = Request.Builder()
//            .url("https://archive.bacs.cs.istu.ru/repository/upload")
//            .post(body)
//            .build()
//        val response = client.newCall(request).execute()
//
//        val document = Jsoup.parse(response.body!!.string())
//        val element = document.body()
//            .getElementsByTag("table")[0]
//            .getElementsByTag("tbody")[0]
//            .getElementsByTag("tr")[1]
//            .getElementsByTag("td")[2]
//            .getElementsByTag("pre")[0]
//            .text()
//        println(element)
//    }
//
//    "Test new api" {
//
//        val API_URL = "http://localhost:8080"
//        val AUTH_USERNAME = "sybon"
//        val AUTH_PASSWORD = "wjh\$42ds09"
//
//        class BasicAuthInjectorInterceptor : Interceptor {
//            override fun intercept(chain: Interceptor.Chain): Response {
//                val credentials = Credentials.basic(AUTH_USERNAME, AUTH_PASSWORD)
//                val request = chain.request().newBuilder().header("Authorization", credentials).build()
//                return chain.proceed(request)
//            }
//        }
//
//        val httpLoggingInterceptor = HttpLoggingInterceptor { message ->
//            getLogger(javaClass).debug(message)
//        }.setLevel(HEADERS)
//        val client = OkHttpClient.Builder()
//            .addInterceptor(BasicAuthInjectorInterceptor())
//            .addInterceptor(httpLoggingInterceptor)
//            .build()
//
//        val zip = Paths.get("4-values-sum-0-low-tl-package174473.zip")
//        val service = BacsArchiveService(client, API_URL.toHttpUrl())
//        service.uploadProblem(zip)
//    }
//
//    "Test post a problem ktor client" {
//
//        val zip = Paths.get("4-values-sum-0-low-tl-package174473.zip")
//
////        val API_URL = "https://archive.bacs.cs.istu.ru/repository/upload"
//        val API_URL = "https://archive.bacs.cs.istu.ru/repository/upload"
////        val AUTH_USERNAME = "sybon"
////        val AUTH_PASSWORD = "wjh\$42ds09"
//        val client = HttpClient {
////            install(Auth) {
////                basic {
////                    sendWithoutRequest = true
////                    username = AUTH_USERNAME
////                    password = AUTH_PASSWORD
////                }
////            }
////            install(Logging) {
////                level = LogLevel.ALL
////            }
////            engine { endpoint { connectAttempts = 3 } }
//        }
//
//        val response = client.post<HttpResponse>(API_URL) {
//            body = MultiPartFormDataContent(
//                formData {
//                    append("_5", "Upload")
//                    append("archiver_format", "")
//                    append("archiver_type", "7z")
//                    append("response", "html")
//                    append("archive", zip.fileName.toString(), ContentType.Application.Zip, zip.fileSize()) {
//                        writeFully(zip.readBytes())
//                    }
//                }
//            )
//            header(HttpHeaders.Host, "archive.bacs.cs.istu.ru")
//            header(HttpHeaders.Authorization, "Basic c3lib246d2poJDQyZHMwOQ==")
//        }
//        println(response.content.toByteArray().decodeToString())
//    }
//
////    "Test get post problem page ktor client" {
////        val API_URL = "https://archive.bacs.cs.istu.ru/repository"
////        val AUTH_USERNAME = "sybon"
////        val AUTH_PASSWORD = "wjh\$42ds09"
////        val client = HttpClient(CIO) {
////            install(Auth) {
////                basic {
////                    sendWithoutRequest = true
////                    username = AUTH_USERNAME
////                    password = AUTH_PASSWORD
////                }
////            }
////            install(Logging) {
////                level = LogLevel.ALL
////            }
////            engine { endpoint { connectAttempts = 3 } }
////        }
////
////        val url = API_URL.toHttpUrl().newBuilder().addEncodedPathSegment("upload").build().toUrl()
////
////        val response = client.get<HttpResponse>(url)
////        println(response.content.toByteArray().decodeToString())
////    }
//})