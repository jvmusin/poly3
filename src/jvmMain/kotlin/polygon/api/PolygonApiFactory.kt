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
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

/**
 * PolygonAPI factory.
 *
 * Used to create [PolygonApi].
 *
 * @property config Polygon API configuration.
 */
class PolygonApiFactory(private val config: PolygonConfig) {

    /**
     * ApiSig adding interceptor.
     *
     * Adds *apiSig* to every request made to Polygon API.
     *
     * Uses [PolygonConfig.apiKey] and [PolygonConfig.secret] to change the request URL.
     */
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

    /**
     * Polygon retry interceptor.
     *
     * Retries the request while [needRepeat] returns *true*.
     *
     * Duration of time it tries to repeat the request and delay between sequential requests
     * are configured via [retryPolicy].
     *
     * @property retryPolicy Configures duration of time it tries to repeat the request
     *                       and delays between sequential requests.
     */
    private abstract class PolygonRetryInterceptor(
        private val retryPolicy: RetryPolicy = RetryPolicy(10.minutes, 1.minutes)
    ) : Interceptor {
        abstract fun needRepeat(response: Response): Boolean

        @Suppress("BlockingMethodInNonBlockingContext")
        override fun intercept(chain: Interceptor.Chain) = runBlocking {
            var done = 0
            retryPolicy.evalWhileFails({ res ->
                if (needRepeat(res)) {
                    val body = res.bufferedBody()?.string() ?: "NO BODY"
                    getLogger(javaClass).warn(
                        "Polygon API responded with code ${res.code} and body '$body'\n" +
                            "Now sleep for ${retryPolicy.retryAfter} and make try #${++done + 1}"
                    )
                    false
                } else true
            }) { chain.proceed(chain.request()) }
        }
    }

    /**
     * Too many requests retry interceptor.
     *
     * Retries the request if Polygon API said TOO MANY REQUESTS.
     */
    private class TooManyRequestsRetryInterceptor : PolygonRetryInterceptor() {
        companion object {
            const val TOO_MANY_REQUESTS_MESSAGE = "Too many requests. Please, wait few seconds and try again"
        }

        override fun needRepeat(response: Response): Boolean {
            return response.code == 400 && response.bufferedBody()?.string() == TOO_MANY_REQUESTS_MESSAGE
        }
    }

    /**
     * Server error retry interceptor.
     *
     * Retries the request if Polygon API responded with 5xx code.
     */
    private class ServerErrorRetryInterceptor : PolygonRetryInterceptor() {
        override fun needRepeat(response: Response) = response.code in 500..599
    }

    /**
     * Changes response code from 400 to 200.
     *
     * Used to treat *code 400* responses as *code 200* responses.
     *
     * Polygon API returns code *400* when something is wrong,
     * but it also returns the message about that in request body,
     * so we will have *null* result and *non-null* message
     * in the [PolygonResponse].
     */
    private class Code400To200Interceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val result = chain.proceed(chain.request())
            return when (result.code) {
                400 -> result.newBuilder().code(200).build()
                else -> result
            }
        }
    }

    /**
     * Creates PolygonAPI using configuration data from [config].
     *
     * @return New PolygonAPI instance.
     */
    fun create(): PolygonApi = RetrofitClientFactory.create(config.url) {
        addInterceptor(Code400To200Interceptor())
        addInterceptor(ServerErrorRetryInterceptor())
        addInterceptor(TooManyRequestsRetryInterceptor())
        addInterceptor(ApiSigAddingInterceptor())
    }
}
