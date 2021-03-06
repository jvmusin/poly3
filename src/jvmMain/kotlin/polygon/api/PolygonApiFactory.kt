@file:OptIn(ExperimentalTime::class, ExperimentalSerializationApi::class)

package polygon.api

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.LoggerFactory.getLogger
import polygon.PolygonConfig
import retrofit.RetrofitClientFactory
import retrofit.bufferedBody
import util.RetryPolicy
import util.sha512
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

class PolygonApiFactory(private val config: PolygonConfig) {

    private inner class ApiSigAddingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val originalUrl = original.url

            val time = System.currentTimeMillis() / 1000
            val prefix = "abcdef"
            val method = originalUrl.pathSegments[1]

            val almostDoneUrl = originalUrl.newBuilder()
                .addQueryParameter("apiKey", config.apiKey)
                .addQueryParameter("time", time.toString())
                .build()

            val middle = almostDoneUrl.queryParameterNames
                .map { it to almostDoneUrl.queryParameter(it) }
                .sortedWith(compareBy({ it.first }, { it.second }))
                .joinToString("&") { "${it.first}=${it.second}" }
            val toHash = "$prefix/$method?$middle#${config.secret}"
            val apiSig = prefix + toHash.sha512()

            val finalUrl = almostDoneUrl.newBuilder().addQueryParameter("apiSig", apiSig).build()
            return chain.proceed(original.newBuilder().url(finalUrl).build())
        }
    }

    private class TooManyRequestsRetryInterceptor(
        private val retryPolicy: RetryPolicy = RetryPolicy(10.minutes, 1.minutes)
    ) : Interceptor {

        companion object {
            const val TOO_MANY_REQUESTS_MESSAGE = "Too many requests. Please, wait few seconds and try again"

            private fun Response.isTooManyRequests() =
                code == 400 && bufferedBody().let { it != null && TOO_MANY_REQUESTS_MESSAGE in it.string() }
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        override fun intercept(chain: Interceptor.Chain) = runBlocking {
            var done = 0
            retryPolicy.evalWhileFails({ res ->
                if (!res.isTooManyRequests()) true
                else {
                    getLogger(javaClass).warn(
                        "Too many requests to Polygon API. " +
                            "Now sleep for ${retryPolicy.retryAfter} and make try #${++done + 1}"
                    )
                    false
                }
            }) { chain.proceed(chain.request()) }
        }
    }

    /**
     * Changes response code from 400 to 200.
     *
     * Used to treat *code 400* responses as *code 200* responses.
     * Since Polygon API returns code 400 when something is wrong,
     * it also returns the message about that in request body,
     * so we will have *null* result and *non-null* message
     * in the [PolygonResponse].
     */
    private class Code400To200Interceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val result = chain.proceed(chain.request())
            if (result.code == 400) {
                return result.newBuilder().code(200).build()
            }
            return result
        }
    }

    private class ServerErrorRetryInterceptor(
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

    fun create(): PolygonApi = RetrofitClientFactory.create(config.url) {
        addInterceptor(Code400To200Interceptor())
        addInterceptor(ServerErrorRetryInterceptor())
        addInterceptor(TooManyRequestsRetryInterceptor())
        addInterceptor(ApiSigAddingInterceptor())
    }
}
