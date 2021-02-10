package bacs

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.logging.HttpLoggingInterceptor
import util.getLogger
import java.util.concurrent.TimeUnit

class BacsArchiveServiceFactory {
    companion object {
        private const val API_URL = "https://archive.bacs.cs.istu.ru/repository"
        private const val AUTH_USERNAME = "sybon"
        private const val AUTH_PASSWORD = "wjh\$42ds09"
    }

    private class BasicAuthInjectorInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val credentials = Credentials.basic(AUTH_USERNAME, AUTH_PASSWORD)
            val request = chain.request().newBuilder().header("Authorization", credentials).build()
            return chain.proceed(request)
        }
    }

    fun create(): BacsArchiveService {
        val httpLoggingInterceptor = HttpLoggingInterceptor { message ->
            getLogger(javaClass).debug(message)
        }.setLevel(HttpLoggingInterceptor.Level.BASIC)
        val dispatcher = Dispatcher().apply {
            maxRequests = 100
            maxRequestsPerHost = 100
        }
        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(BasicAuthInjectorInterceptor())
            .addInterceptor(httpLoggingInterceptor)
            .dispatcher(dispatcher)
            .build()
        return BacsArchiveService(client, API_URL.toHttpUrl())
    }
}