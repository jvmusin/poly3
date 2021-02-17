package server

import api.Toast
import api.ToastKind
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.sessions.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import util.getLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

interface MessageSenderFactory {
    fun create(context: PipelineContext<*, ApplicationCall>, title: String): MessageSender
    fun create(session: WebSocketServerSession, title: String): MessageSender
    fun registerClient(session: WebSocketServerSession)
}

class MessageSenderFactoryImpl : MessageSenderFactory {
    private val wsBySession = ConcurrentHashMap<Session, CopyOnWriteArrayList<SendChannel<Frame>>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun CoroutineScope.createMessageSender(call: ApplicationCall, title: String) = object : MessageSender {
        override fun invoke(content: String, kind: ToastKind) {
            val list = wsBySession[call.sessions.get<Session>()]!!
            list.removeIf { it.isClosedForSend }
            for (out in list) {
                launch {
                    try {
                        out.send(Frame.Text(Json.encodeToString(Toast(title, content, kind))))
                    } catch (e: Exception) {
                        getLogger(javaClass).warn("Уведомление не отправлено", e)
                    }
                }
            }
        }
    }

    override fun create(context: PipelineContext<*, ApplicationCall>, title: String): MessageSender {
        return context.createMessageSender(context.call, title)
    }

    override fun create(session: WebSocketServerSession, title: String): MessageSender {
        return session.createMessageSender(session.call, title)
    }

    override fun registerClient(session: WebSocketServerSession) {
        wsBySession.computeIfAbsent(session.call.sessions.get()!!) { CopyOnWriteArrayList() }
            .add(session.outgoing)
    }
}