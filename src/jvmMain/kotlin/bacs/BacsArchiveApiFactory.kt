package bacs

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import util.getLogger
import java.util.concurrent.TimeUnit

class BacsArchiveApiFactory {
    companion object {
        const val API_URL = "https://archive.bacs.cs.istu.ru/repository/"
        const val AUTH_TOKEN = "Basic c3lib246d2poJDQyZHMwOQ=="
    }

    private class BasicAuthInjectorInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            return chain.proceed(chain.request().newBuilder().header("Authorization", AUTH_TOKEN).build())
        }
    }

    private fun buildClient(): OkHttpClient {
        val httpLoggingInterceptor = HttpLoggingInterceptor { message ->
            getLogger(HttpLoggingInterceptor::class.java).debug(message)
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