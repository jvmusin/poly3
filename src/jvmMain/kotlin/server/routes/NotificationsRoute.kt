package server.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.koin.ktor.ext.inject
import server.MessageSenderFactory
import server.Session
import util.getLogger
import java.util.*

fun Route.notifications() {
    val messageSenderFactory: MessageSenderFactory by inject()

    get("register-session") {
        getLogger(javaClass).info("Registering session")
        call.sessions.getOrSet { Session(UUID.randomUUID().toString()) }
        getLogger(javaClass).info("Registered session")
        call.respond(HttpStatusCode.OK)
    }
    webSocket("subscribe") {
        getLogger(javaClass).info("Subscribing WS")
        messageSenderFactory.registerClient(this)
        try {
            getLogger(javaClass).info("Subscribed WS")
            incoming.receive()  // block connection
        } catch (e: ClosedReceiveChannelException) {
            getLogger(javaClass).info("WS connection closed: ${e.message}")
        }
    }
    post("bump-test-notification") {
        messageSenderFactory.create(this, "Привет!")("Хорошо сейчас на улице, выйди прогуляйся")
        call.respond(HttpStatusCode.OK)
    }
}