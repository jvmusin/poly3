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
import org.slf4j.event.Level
import polygon.polygonModule
import server.routes.routes
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
            MessageSenderFactory.createMessageSender(call)("Ошибка", cause.message.orEmpty(), ToastKind.FAILURE)
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "No message provided")
            throw cause
        }
    }
    install(DefaultHeaders)
    install(WebSockets)
    install(Sessions) {
        cookie<Session>("SESSION")
    }
    install(Koin) {
        modules(retrofitModule, sybonModule, bacsModule, polygonModule)
    }

    routing {
        routes()
    }
}