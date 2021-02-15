package sybon

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import util.getLogger
import java.util.concurrent.TimeUnit

class SybonApiFactory {
    companion object {
        private const val ARCHIVE_API_URL = "https://archive.sybon.org/api/"
        private const val CHECKING_API_URL = "https://checking.sybon.org/api/"

        @Suppress("SpellCheckingInspection")
        private const val API_KEY = "YBJY9zkkUUigNcYOlFoSg"
    }

    private class ApiKeyInjectorInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val newUrl = chain.request().url.newBuilder().addQueryParameter("api_key", API_KEY).build()
            val newRequest = chain.request().newBuilder().url(newUrl).build()
            return chain.proceed(newRequest)
        }
    }

    private fun buildClient(): OkHttpClient {
        val httpLoggingInterceptor = HttpLoggingInterceptor { message ->
            getLogger(HttpLoggingInterceptor::class.java).info(message)
        }.setLevel(HttpLoggingInterceptor.Level.BASIC)
        val dispatcher = Dispatcher().apply {
            maxRequests = 100
            maxRequestsPerHost = 100
        }
        return OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(ApiKeyInjectorInterceptor())
            .addInterceptor(httpLoggingInterceptor)
            .dispatcher(dispatcher)
            .build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun buildRetrofit(url: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url)
            .client(buildClient())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(Json { isLenient = true }.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private fun <T> buildApi(url: String, clazz: Class<T>): T = buildRetrofit(url).create(clazz)

    fun createArchiveApi() = buildApi(ARCHIVE_API_URL, SybonArchiveApi::class.java)
    fun createCheckingApi() = buildApi(CHECKING_API_URL, SybonCheckingApi::class.java)
}