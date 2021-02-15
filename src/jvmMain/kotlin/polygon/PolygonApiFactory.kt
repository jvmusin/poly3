@file:OptIn(ExperimentalTime::class, ExperimentalSerializationApi::class)

package polygon

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
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
import util.getLogger
import util.sha512
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

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
        private val period: Duration = 1.minutes,
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
                            "Too many requests to Polygon API. Now sleep for $period and make try #${done + 1} of $retries"
                        )
                        Thread.sleep(period.toLongMilliseconds()) // we can't delay the coroutine here since intercept is not a suspend function =(
                        continue
                    }
                    return res.newBuilder().body(body.toResponseBody()).build()
                }
                return res
            }
        }
    }

    private class Error500RetryInterceptor(
        private val period: Duration = 1.minutes,
        private val retries: Int = 10
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var done = 0
            while (true) {
                val res = chain.proceed(chain.request())
                if ((res.code in 500..599) && done++ < retries) {
                    getLogger(javaClass).warn(
                        "Polygon API responded with ${res.code} error. Now sleep for $period and make try #${done + 1} of $retries"
                    )
                    val body = res.body!!.bytes().decodeToString()
                    getLogger(javaClass).warn(
                        "Responded with this text: $body"
                    )
                    continue
                }
                return res
            }
        }
    }

    fun create(): PolygonApi {
        val httpLoggingInterceptor = HttpLoggingInterceptor { message ->
            getLogger(HttpLoggingInterceptor::class.java).info(message)
        }.setLevel(HttpLoggingInterceptor.Level.BASIC)
        val dispatcher = Dispatcher().apply {
            maxRequests = 15
            maxRequestsPerHost = 15
        }
        val client = OkHttpClient().newBuilder()
            .connectTimeout(2, TimeUnit.MINUTES)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .addInterceptor(TooManyRequestsRetryInterceptor())
            .addInterceptor(Error500RetryInterceptor())
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