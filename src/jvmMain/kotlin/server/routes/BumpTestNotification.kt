package server.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import server.MessageSenderFactory.createMessageSender

fun Route.bumpTestNotification() {
    post("bump-test-notification") {
        createMessageSender()("Привет!", "Хорошо сейчас на улице, выйди прогуляйся")
        call.respond(HttpStatusCode.OK)
    }
}