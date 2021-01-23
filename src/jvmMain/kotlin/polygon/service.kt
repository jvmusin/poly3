package polygon

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.security.MessageDigest

const val POLYGON_KEY = "39f8cd6bb1f5b79054fb69623c624b4b331cd6b6"
const val POLYGON_SECRET = "c2a453543589c5650131b9e2fa8d186ca3ae01b4"
const val POLYGON_URL = "https://polygon.codeforces.com/api/"

fun String.sha512(): String {
    val digest = MessageDigest.getInstance("SHA-512").digest(toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

object ApiSigAddingInterceptor : Interceptor {
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

fun buildPolygonService(): PolygonService {
    val client = OkHttpClient().newBuilder()
        .addInterceptor(ApiSigAddingInterceptor)
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .build()
    val contentType = "application/json".toMediaType()
    val retrofit = Retrofit.Builder()
        .baseUrl(POLYGON_URL)
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(Json { isLenient = true }.asConverterFactory(contentType))
        .build()
    return PolygonService(
        retrofit.create(PolygonProblemsService::class.java),
        retrofit.create(PolygonProblemService::class.java),
        retrofit.create(PolygonContestService::class.java),
    )
}
