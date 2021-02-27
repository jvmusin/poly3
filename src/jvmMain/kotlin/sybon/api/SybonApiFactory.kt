package sybon.api

import okhttp3.Interceptor
import okhttp3.Response
import retrofit.RetrofitClientFactory
import sybon.SybonConfig

class SybonApiFactory(private val config: SybonConfig) {

    private inner class ApiKeyInjectorInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val newUrl = chain.request().url.newBuilder().addQueryParameter("api_key", config.apiKey).build()
            val newRequest = chain.request().newBuilder().url(newUrl).build()
            return chain.proceed(newRequest)
        }
    }

    private inline fun <reified T> createApi(url: String): T = RetrofitClientFactory.create(url) {
        addInterceptor(ApiKeyInjectorInterceptor())
    }

    fun createArchiveApi(): SybonArchiveApi = createApi(config.archiveApiUrl)
    fun createCheckingApi(): SybonCheckingApi = createApi(config.checkingApiUrl)
}
