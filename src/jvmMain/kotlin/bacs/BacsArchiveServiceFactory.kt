package bacs

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.logging.*
import io.ktor.http.URLProtocol.Companion.HTTPS
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

class BacsArchiveServiceFactory {
    companion object {
        private val PROTOCOL = HTTPS
        private const val HOST = "archive.bacs.cs.istu.ru"
        private const val BASE_PATH = "repository"
        private const val AUTH_USERNAME = "sybon"
        private const val AUTH_PASSWORD = "wjh\$42ds09"
    }

    @OptIn(ExperimentalTime::class)
    fun create(): BacsArchiveServiceImpl {
        val client = HttpClient(CIO) {
            install(Auth) {
                basic {
                    sendWithoutRequest = true
                    username = AUTH_USERNAME
                    password = AUTH_PASSWORD
                }
            }
            install(Logging) { level = LogLevel.INFO }
            defaultRequest {
                url {
                    protocol = PROTOCOL
                    host = HOST
                    encodedPath = "$BASE_PATH/$encodedPath"
                }
            }
            engine {
                this.requestTimeout = 2.minutes.toLongMilliseconds()
            }
        }
        return BacsArchiveServiceImpl(client)
    }
}