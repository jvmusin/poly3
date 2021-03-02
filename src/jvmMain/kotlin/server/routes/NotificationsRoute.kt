package server.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.sessions.getOrSet
import io.ktor.sessions.sessions
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.koin.ktor.ext.inject
import server.MessageSenderFactory
import server.Session
import util.getLogger
import java.util.UUID

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
            incoming.receive() // block connection
        } catch (e: ClosedReceiveChannelException) {
            getLogger(javaClass).info("WS connection closed: ${e.message}")
        }
    }
    post("bump-test-notification") {
        messageSenderFactory.create(this, "Привет!")("Хорошо сейчас на улице, выйди прогуляйся")
        call.respond(HttpStatusCode.OK)
    }
}
