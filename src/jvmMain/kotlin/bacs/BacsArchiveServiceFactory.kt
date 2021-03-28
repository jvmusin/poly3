package bacs

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.basic
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.http.URLProtocol.Companion.HTTPS
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

/** Factory for [BacsArchiveService]. */
class BacsArchiveServiceFactory(private val config: BacsConfig) {

    /** Creates [BacsArchiveService] using auth credentials and other settings from [config]. */
    @OptIn(ExperimentalTime::class)
    fun create(): BacsArchiveService {
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
        return BacsArchiveService(client)
    }
}
