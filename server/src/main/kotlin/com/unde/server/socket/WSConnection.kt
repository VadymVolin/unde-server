package com.unde.server.socket

import com.unde.server.socket.model.WSMessage
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json

internal class WSConnection (
    val id: String,
    private val session: DefaultWebSocketServerSession,
    private val onClose: (WSConnection) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { classDiscriminator = "type" }

    fun start() {
        scope.launch {
            try {
                session.incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val message = frame.readText()
                        println("[$id] Received: $message")

                        val decoded = json.decodeFromString<WSMessage>(message)
                        handleMessage(decoded)
                    }
                }
            } catch (e: Exception) {
                println("[$id] Error: ${e.message}")
            } finally {
                println("[$id] Disconnected.")
                cleanup()
            }
        }
    }

    suspend fun send(message: WSMessage) {
        try {
            if (session.isActive) {
                session.send(Frame.Text(json.encodeToString(WSMessage.serializer(), message)))
            }
        } catch (e: Exception) {
            println("[$id] Send error: ${e.message}")
        }
    }

    private fun handleMessage(msg: WSMessage) {
        when (msg) {
            is WSMessage.Command -> {
                println("[$id] Command: ${msg.name}")
                // Do something and respond
                scope.launch {
                    send(WSMessage.Response("ok", "Executed ${msg.name}"))
                }
            }

            else -> println("[$id] Unhandled: $msg")
        }
    }

    fun cleanup() {
        scope.cancel()
        onClose(this)
    }
}
