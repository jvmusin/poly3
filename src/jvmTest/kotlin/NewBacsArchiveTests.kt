import bacs.BacsArchiveService
import bacs.BacsArchiveServiceFactory
import io.kotest.core.spec.style.StringSpec
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.logging.*
import io.ktor.http.*
import java.nio.file.Paths

const val AUTH_USERNAME = "sybon"
const val AUTH_PASSWORD = "wjh\$42ds09"

class NewBacsArchiveTests : StringSpec({
    "Upload a problem" {
        val client = HttpClient(CIO) {
            install(Auth) {
                basic {
                    username = AUTH_USERNAME
                    password = AUTH_PASSWORD
                }
            }
            install(Logging) { level = LogLevel.ALL }
            defaultRequest {
                this.url {
                    protocol = URLProtocol.HTTPS
                    host = "archive.bacs.cs.istu.ru"
                    encodedPath = "repository/$encodedPath"
                }
            }
        }
        val service = BacsArchiveService(client)
        val zip = Paths.get("4-values-sum-0-low-tl-package174473.zip")
        service.uploadProblem(zip)
    }

    "Check problem import result" {
        val client = BacsArchiveServiceFactory().create()

    }
})