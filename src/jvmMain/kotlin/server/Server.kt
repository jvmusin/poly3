package server

import api.ToastKind
import bacs.bacsModule
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.features.gzip
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.timeout
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.netty.EngineMain
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.websocket.WebSockets
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.get
import org.slf4j.event.Level
import polygon.polygonModule
import server.routes.root
import sybon.sybonModule
import kotlin.time.ExperimentalTime
import kotlin.time.days
import kotlin.time.seconds
import kotlin.time.toJavaDuration

data class Session(val str: String)

fun main(args: Array<String>): Unit = EngineMain.main(args)

@OptIn(ExperimentalTime::class)
@Suppress("unused")
fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(Compression) {
        gzip()
    }
    install(CallLogging) {
        level = Level.INFO
    }
    install(StatusPages) {
        exception<Throwable> { cause ->
            val msg = cause.message.orEmpty()
            get<MessageSenderFactory>().create(this, "Ошибка")(msg, ToastKind.FAILURE)
            call.respond(HttpStatusCode.BadRequest, msg)
            throw cause
        }
    }
    install(DefaultHeaders)
    install(WebSockets) {
        pingPeriod = 10.seconds.toJavaDuration()
        timeout = 1.days.toJavaDuration()
    }
    install(Sessions) {
        cookie<Session>("SESSION")
    }
    install(Koin) {
        val config = ConfigFactory.load()
        modules(
            sybonModule(config.extract("sybon")),
            bacsModule(config.extract("bacs")),
            polygonModule(config.extract("polygon")),
            serverModule
        )
    }

    routing {
        root()
    }
}
