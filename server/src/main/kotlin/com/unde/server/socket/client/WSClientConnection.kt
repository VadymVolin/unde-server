package com.unde.server.socket.client

import com.unde.server.constants.JsonToken
import com.unde.server.socket.client.model.WSClientMessage
import com.unde.server.socket.library.WSLibraryConnectionBroker
import io.ktor.server.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import java.util.UUID

internal class WSClientConnection(
    val id: String = UUID.randomUUID().toString(),
    private val session: DefaultWebSocketServerSession,
    private val onDisconnect: (WSClientConnection) -> Unit
) {
    private val logger = KtorSimpleLogger(javaClass.simpleName)
    private val json = Json { classDiscriminator = JsonToken.TYPE_TOKEN }

    internal suspend fun connect() {
        logger.info("Client UI connection [$id] established")
        try {
            // Send initial connections list
            val connections = WSLibraryConnectionBroker.getActiveConnections()
            send(WSClientMessage.ConnectionsList(connections))
            
            // Handle incoming messages
            session.incoming.consumeEach(::handleMessage)
        } catch (e: Exception) {
            logger.info("Client UI [$id] exception: ${e.message}")
        } finally {
            logger.info("Client UI [$id] disconnected")
            onDisconnect(this@WSClientConnection)
        }
    }

    internal suspend fun send(message: WSClientMessage) {
        try {
            if (session.isActive) {
                session.send(Frame.Text(json.encodeToString(WSClientMessage.serializer(), message)))
            }
        } catch (e: Exception) {
            logger.error("Cannot send message to client UI [$id]: ${e.message}")
        }
    }

    private fun handleMessage(frame: Frame) {
        if (frame is Frame.Text) {
            val text = frame.readText()
            try {
                val message = json.decodeFromString<WSClientMessage>(text)
                logger.info("Message from client UI [$id]: $text")
                when (message) {
                    is WSClientMessage.SelectConnection -> {
                        logger.info("Client selected connection: ${message.connectionId}")
                        // Client selected a connection - could send historical data here if needed
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
