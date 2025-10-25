package com.unde.server.socket

import com.unde.server.constants.JsonToken
import com.unde.server.socket.model.WSMessage
import io.ktor.server.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

internal class WSConnection(
    val id: String,
    private val session: DefaultWebSocketServerSession,
    private val onDisconnect: (WSConnection) -> Unit
) {
    private val logger = KtorSimpleLogger(javaClass.simpleName)

    val json = Json { classDiscriminator = JsonToken.TYPE_TOKEN }

    internal suspend fun connect() {
        logger.info("The connection to device[$id] has been established")
        try {
            session.send(Frame.Text(Json.encodeToString<WSMessage>(WSMessage.Command(JsonObject(mapOf(JsonToken.RESULT_TOKEN to Json.encodeToJsonElement("Connected successfully!")))))))
            session.incoming.consumeEach { frame ->
                handleMessage(frame)
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
                session.send(Frame.Text(json.encodeToString(WSMessage.serializer(), message)))
            }
        } catch (e: Exception) {
            logger.error("Cannot send message to device[$id]: ${e.message}")
        }
    }

    private fun handleMessage(frame: Frame) {
        if (frame is Frame.Text) {
            val text = frame.readText()
            try {
                val message = json.decodeFromString<WSMessage>(text)
                logger.info("Message from device[$id] has been received: $text")
                when (message) {
                    is WSMessage.Command -> {
                        logger.info("Received COMMAND message: ${message.data}")
                    }
                    is WSMessage.Network -> {
                        logger.info("Received NETWORK message: ${message.data}")
                    }
                    is WSMessage.Telemetry -> {
                        logger.info("Received TELEMETRY message: ${message.data}")
                    }
                    is WSMessage.Logcat -> {
                        logger.info("Received LOGCAT message: ${message.data}")
                    }
                    is WSMessage.Database -> {
                        logger.info("Received DATABASE message: ${message.data}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to parse WSMessage: ${e.message}")
            }
        }
    }
}
