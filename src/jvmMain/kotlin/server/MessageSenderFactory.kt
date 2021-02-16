package server

import io.ktor.application.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*

interface MessageSenderFactory {
    fun create(context: PipelineContext<*, ApplicationCall>): MessageSender
    fun create(session: WebSocketServerSession): MessageSender
    fun registerClient(session: WebSocketServerSession)
}