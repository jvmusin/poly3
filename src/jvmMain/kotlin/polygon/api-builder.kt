package polygon

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import getLogger
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import sha512
import java.util.concurrent.TimeUnit
import kotlin.time.seconds
import kotlin.time.toJavaDuration

private const val POLYGON_KEY = "39f8cd6bb1f5b79054fb69623c624b4b331cd6b6"
private const val POLYGON_SECRET = "c2a453543589c5650131b9e2fa8d186ca3ae01b4"
private const val POLYGON_URL = "https://polygon.codeforces.com/api/"

private object ApiSigAddingInterceptor : Interceptor {
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

fun buildPolygonApi(): PolygonApi {
    val httpLoggingInterceptor = HttpLoggingInterceptor { message ->
        getLogger(HttpLoggingInterceptor::class.java).debug(message)
    }.setLevel(HttpLoggingInterceptor.Level.BASIC)
    val client = OkHttpClient().newBuilder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(ApiSigAddingInterceptor)
        .addInterceptor(httpLoggingInterceptor)
        .build()
    val contentType = "application/json".toMediaType()
    val retrofit = Retrofit.Builder()
        .baseUrl(POLYGON_URL)
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(Json { isLenient = true }.asConverterFactory(contentType))
        .build()
    val problemApi = retrofit.create(ProblemApi::class.java)
    val contestApi = retrofit.create(ContestApi::class.java)
    return object : PolygonApi {
        override val problem: ProblemApi get() = problemApi
        override val contest: ContestApi get() = contestApi
    }
}