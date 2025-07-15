package com.unde.server.socket

import com.unde.server.socket.model.WSMessage
import io.ktor.server.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive

internal class WSConnection(
    val id: String,
    private val session: DefaultWebSocketServerSession,
    private val onDisconnect: (WSConnection) -> Unit
) {
    private val logger = KtorSimpleLogger(javaClass.simpleName)

    internal suspend fun connect() {
        logger.info("The connection to device[$id] has been established")
        try {
            session.incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val message = frame.readText()
                    logger.info("Message from device[$id] has been received: $message")
//                        val decoded = json.decodeFromString<WSMessage>(message)
//                        handleMessage(decoded)
                }
            }
        } catch (e: Exception) {
            logger.info("Exception has been caught, device[$id]: ${e.message}")
        } finally {
            logger.info("Device[$id] has been disconnected")
            onDisconnect(this@WSConnection)
        }
    }

    internal suspend fun send(message: WSMessage) {
        try {
            if (session.isActive) {
//                session.send(Frame.Text(json.encodeToString(WSMessage.serializer(), message)))
            }
        } catch (e: Exception) {
            logger.error("Cannot send message to device[$id]: ${e.message}")
        }
    }

    private fun handleMessage(msg: WSMessage) {
//        when (msg) {
//            is WSMessage.Command -> {
//                println("[$id] Command: ${msg.name}")
//                // Do something and respond
//                scope.launch {
//                    send(WSMessage.Response("ok", "Executed ${msg.name}"))
//                }
//            }
//
//            else -> println("[$id] Unhandled: $msg")
//        }
    }
}
