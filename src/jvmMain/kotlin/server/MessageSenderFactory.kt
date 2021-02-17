package server

import io.ktor.application.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*

interface MessageSenderFactory {
    fun create(context: PipelineContext<*, ApplicationCall>, title: String): MessageSender
    fun create(session: WebSocketServerSession, title: String): MessageSender
    fun registerClient(session: WebSocketServerSession)
}