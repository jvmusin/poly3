package server.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import server.MessageSenderFactory
import server.Session
import util.getLogger
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@OptIn(ExperimentalTime::class)
fun Route.notifications() {
    get("register-session") {
        getLogger(javaClass).info("Registering session")
        call.sessions.getOrSet { Session(UUID.randomUUID().toString()) }
        getLogger(javaClass).info("Registered session")
        call.respond(HttpStatusCode.OK)
    }
    webSocket("subscribe") {
        pingInterval = 10.seconds.toJavaDuration()
        getLogger(javaClass).info("Subscribing WS")
        MessageSenderFactory.registerClient(this)
        getLogger(javaClass).info("Subscribed WS")
        try {
            val receive = incoming.receive()
            println(receive.frameType)
            if (receive.frameType == FrameType.TEXT) {
                println(receive.readBytes().decodeToString())
            }
        } catch (e: ClosedReceiveChannelException) {
            getLogger(javaClass).info("WS connection closed: ${e.message}")
        }
    }
}