package server

import api.ToastKind
import bacs.bacsModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
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
import util.retrofitModule

data class Session(val str: String)

fun main(args: Array<String>): Unit = EngineMain.main(args)

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
    install(WebSockets)
    install(Sessions) {
        cookie<Session>("SESSION")
    }
    install(Koin) {
        modules(retrofitModule, sybonModule, bacsModule, polygonModule, serverModule)
    }

    routing {
        root()
    }
}