package retrofit

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory.getLogger
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit

object RetrofitClientFactory {
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> create(baseUrl: String, builder: OkHttpClient.Builder.() -> Unit): T {
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
            .apply(builder)
            .addInterceptor(httpLoggingInterceptor)
            .dispatcher(dispatcher)
            .build()
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(Json { isLenient = true }.asConverterFactory(contentType))
            .build()
            .create()
    }
}
