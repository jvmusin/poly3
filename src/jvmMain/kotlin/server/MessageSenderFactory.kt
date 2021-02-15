@file:OptIn(ExperimentalCoroutinesApi::class)

package server

import Session
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import util.getLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object MessageSenderFactory {
    private val wsBySession = ConcurrentHashMap<String, CopyOnWriteArrayList<SendChannel<Frame>>>()
    private fun CoroutineScope.createMessageSender(call: ApplicationCall) = object : MessageSender {
        override fun invoke(title: String, content: String, kind: ToastKind) {
            val list = wsBySession[call.sessions.get<Session>()!!.str]!!
            list.removeIf { it.isClosedForSend }
            for (out in list) {
                launch {
                    try {
                        out.send(Frame.Text(Json.encodeToString(Toast(title, content, kind))))
                    } catch (e: Exception) {
                        getLogger(javaClass).warn(e.message)
                    }
                }
            }
        }
    }

    fun PipelineContext<Unit, ApplicationCall>.createMessageSender() = createMessageSender(call)
    fun DefaultWebSocketServerSession.createMessageSender() = createMessageSender(call)
    suspend fun createMessageSender(call: ApplicationCall) = coroutineScope { createMessageSender(call) }

    fun registerClient(session: DefaultWebSocketServerSession) {
        wsBySession.computeIfAbsent(session.call.sessions.get<Session>()!!.str) { CopyOnWriteArrayList() }
            .add(session.outgoing)
    }
}