package server

import api.ToastKind
import bacs.bacsModule
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.netty.*
import io.ktor.sessions.*
import io.ktor.websocket.*
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
        modules(sybonModule, bacsModule(config.extract("bacs")), polygonModule(config.extract("polygon")), serverModule)
    }

    routing {
        root()
    }
}