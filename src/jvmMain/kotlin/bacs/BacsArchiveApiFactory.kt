package bacs

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import util.getLogger
import java.util.concurrent.TimeUnit

class BacsArchiveApiFactory {
    companion object {
        const val API_URL = "https://archive.bacs.cs.istu.ru/repository/"
        const val AUTH_USERNAME = "sybon"
        const val AUTH_PASSWORD = "wjh\$42ds09"
    }

    private class BasicAuthInjectorInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val credentials = Credentials.basic(AUTH_USERNAME, AUTH_PASSWORD)
            val request = chain.request().newBuilder().header("Authorization", credentials).build()
            return chain.proceed(request)
        }
    }

    private fun buildClient(): OkHttpClient {
        val httpLoggingInterceptor = HttpLoggingInterceptor { message ->
            getLogger(javaClass).debug(message)
        }.setLevel(HttpLoggingInterceptor.Level.BASIC)
        val dispatcher = Dispatcher().apply {
            maxRequests = 100
            maxRequestsPerHost = 100
        }
        return OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(BasicAuthInjectorInterceptor())
            .addInterceptor(httpLoggingInterceptor)
            .dispatcher(dispatcher)
            .build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun create(): BacsArchiveApi {
        return Retrofit.Builder()
            .baseUrl(API_URL)
            .client(buildClient())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(Json { isLenient = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create()
    }
}