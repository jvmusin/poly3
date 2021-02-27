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

class BacsArchiveServiceFactory(private val config: BacsConfig) {

    @OptIn(ExperimentalTime::class)
    fun create(): BacsArchiveServiceImpl {
        val client = HttpClient(CIO) {
            install(Auth) {
                basic {
                    sendWithoutRequest = true
                    username = config.username
                    password = config.password
                }
            }
            install(Logging) { level = LogLevel.INFO }
            defaultRequest {
                url {
                    protocol = HTTPS
                    host = config.host
                    encodedPath = "${config.basePath}/$encodedPath"
                }
            }
            engine {
                this.requestTimeout = 2.minutes.toLongMilliseconds()
            }
        }
        return BacsArchiveServiceImpl(client)
    }
}