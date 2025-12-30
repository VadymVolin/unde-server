package com.unde.server.socket.client

import com.unde.server.constants.JsonToken
import com.unde.server.socket.client.model.WSLocalMessage
import com.unde.server.socket.remote.WSRemoteConnectionBroker
import io.ktor.server.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import java.util.UUID

internal class WSLocalConnection(
    val id: String = UUID.randomUUID().toString(),
    private val session: DefaultWebSocketServerSession,
    private val onDisconnect: (WSLocalConnection) -> Unit
) {
    private val logger = KtorSimpleLogger(javaClass.simpleName)
    private val json = Json { classDiscriminator = JsonToken.TYPE_TOKEN }

    internal suspend fun connect() {
        logger.info("Client UI connection [$id] established")
        try {
            // Send initial connections list
            send(WSLocalMessage.ConnectionsList(WSRemoteConnectionBroker.getActiveConnections()))
            // Handle incoming messages
            session.incoming.consumeEach(::handleMessage)
        } catch (e: Exception) {
            logger.info("Client UI [$id] exception: ${e.message}")
        } finally {
            logger.info("Client UI [$id] disconnected")
            onDisconnect(this)
        }
    }

    internal suspend fun send(message: WSLocalMessage) {
        try {
            if (session.isActive) {
                session.send(Frame.Text(json.encodeToString(WSLocalMessage.serializer(), message)))
            }
        } catch (e: Exception) {
            logger.error("Cannot send message to client UI [$id]: ${e.message}")
        }
    }

    private fun handleMessage(frame: Frame) {
        if (frame is Frame.Text) {
            val text = frame.readText()
            try {
                val message = json.decodeFromString<WSLocalMessage>(text)
                logger.info("Message from client UI [$id]: $text")
                when (message) {
                    is WSLocalMessage.SelectConnection -> {
                        logger.info("Client selected connection: ${message.connectionId}")
                        send(WSLocalMessage.Network())
                    }
                    else -> {
                        logger.warn("Unexpected message type from client: ${message::class.simpleName}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to parse WSClientMessage: ${e.message}")
            }
        }
    }
}