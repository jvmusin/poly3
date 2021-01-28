package polygon

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import util.getLogger
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import util.sha512
import java.util.concurrent.TimeUnit

class PolygonApiFactory {

    companion object {
        private const val POLYGON_KEY = "39f8cd6bb1f5b79054fb69623c624b4b331cd6b6"
        private const val POLYGON_SECRET = "c2a453543589c5650131b9e2fa8d186ca3ae01b4"
        private const val POLYGON_URL = "https://polygon.codeforces.com/api/"
    }

    private class ApiSigAddingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val originalUrl = original.url

            val time = System.currentTimeMillis() / 1000
            val prefix = "abcdef"
            val method = originalUrl.pathSegments[1]

            val almostDoneUrl = originalUrl.newBuilder()
                .addQueryParameter("apiKey", POLYGON_KEY)
                .addQueryParameter("time", time.toString())
                .build()

            val middle = almostDoneUrl.queryParameterNames
                .map { it to almostDoneUrl.queryParameter(it) }
                .sortedWith(compareBy({ it.first }, { it.second }))
                .joinToString("&") { "${it.first}=${it.second}" }
            val toHash = "$prefix/$method?$middle#$POLYGON_SECRET"
            val apiSig = prefix + toHash.sha512()

            val finalUrl = almostDoneUrl.newBuilder().addQueryParameter("apiSig", apiSig).build()
            return chain.proceed(original.newBuilder().url(finalUrl).build())
        }
    }

    private class TooManyRequestsRetryInterceptor(
        private val periodSeconds: Int = 60,
        private val retries: Int = 10
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var done = 0
            while (true) {
                val res = chain.proceed(chain.request())
                if (res.code == 400 && res.body != null && done++ < retries) {
                    val body = res.body!!.bytes().decodeToString()
                    if (body.contains("Too many requests. Please, wait few seconds and try again")) {
                        getLogger(javaClass).warn(
                            "Too many requests to Polygon API. Now sleep for $periodSeconds seconds and make try #${done + 1}"
                        )
                        Thread.sleep(periodSeconds * 1000L) // we can't delay coroutine here since it's not a suspend function =(
                        continue
                    }
                    return res.newBuilder().body(body.toResponseBody()).build()
                }
                return res
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun create(): PolygonApi {
        val httpLoggingInterceptor = HttpLoggingInterceptor { message ->
            getLogger(HttpLoggingInterceptor::class.java).debug(message)
        }.setLevel(HttpLoggingInterceptor.Level.BASIC)
        val dispatcher = Dispatcher().apply {
            maxRequests = 100
            maxRequestsPerHost = 100
        }
        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(TooManyRequestsRetryInterceptor())
            .addInterceptor(ApiSigAddingInterceptor())
            .addInterceptor(httpLoggingInterceptor)
            .dispatcher(dispatcher)
            .build()
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(POLYGON_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(Json { isLenient = true }.asConverterFactory(contentType))
            .build()
        return retrofit.create(PolygonApi::class.java)
    }
}