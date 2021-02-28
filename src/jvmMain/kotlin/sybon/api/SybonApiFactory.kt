package sybon.api

import okhttp3.Interceptor
import okhttp3.Response
import retrofit.RetrofitClientFactory
import sybon.SybonConfig

/**
 * Sybon API factory.
 *
 * Used to create [SybonArchiveApi] and [SybonCheckingApi].
 *
 * It uses [RetrofitClientFactory] under the hood to make the actual requests.
 *
 * @constructor Creates Sybon API factory.
 * @property config Sybon configuration, used to configure proper *apiKey* and system urls.
 */
class SybonApiFactory(private val config: SybonConfig) {

    /**
     * ApiKey injector interceptor
     *
     * Injects *apiKey* to every request made to the API.
     *
     * @constructor Creates *apiKey* injector interceptor.
     */
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
