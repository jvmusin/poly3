package sybon.api

import okhttp3.Interceptor
import okhttp3.Response
import retrofit.RetrofitClientFactory

class SybonApiFactory {
    companion object {
        private const val ARCHIVE_API_URL = "https://archive.sybon.org/api/"
        private const val CHECKING_API_URL = "https://checking.sybon.org/api/"

        @Suppress("SpellCheckingInspection")
        private const val API_KEY = "YBJY9zkkUUigNcYOlFoSg"
    }

    private object ApiKeyInjectorInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val newUrl = chain.request().url.newBuilder().addQueryParameter("api_key", API_KEY).build()
            val newRequest = chain.request().newBuilder().url(newUrl).build()
            return chain.proceed(newRequest)
        }
    }

    private inline fun <reified T> createApi(url: String): T = RetrofitClientFactory.create(url) {
        addInterceptor(ApiKeyInjectorInterceptor)
    }

    fun createArchiveApi(): SybonArchiveApi = createApi(ARCHIVE_API_URL)
    fun createCheckingApi(): SybonCheckingApi = createApi(CHECKING_API_URL)
}