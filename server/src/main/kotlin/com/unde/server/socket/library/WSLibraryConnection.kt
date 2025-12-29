package com.unde.server.socket.library

import com.unde.server.constants.JsonToken
import com.unde.server.constants.Text
import com.unde.server.socket.client.WSClientConnectionBroker
import com.unde.server.socket.client.data.WSClientConnectionDataStore
import com.unde.server.socket.library.model.WSLibraryMessage
import com.unde.server.socket.library.model.createResultCommandData
import io.ktor.server.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

internal class WSLibraryConnection(
    val id: String,
    private val session: DefaultWebSocketServerSession,
    private val onDisconnect: (WSLibraryConnection) -> Unit
) {
    private val logger = KtorSimpleLogger(javaClass.simpleName)

    val json = Json { classDiscriminator = JsonToken.TYPE_TOKEN }

    internal suspend fun connect() {
        logger.info("The connection to device[$id] has been established")
        try {
            send(WSLibraryMessage.Command(createResultCommandData(Text.CONNECTED_SUCCESSFULLY)))
            for (frame in session.incoming) {
                handleMessage(frame)
            }
        } catch (e: Exception) {
            logger.info("Exception has been caught, device[$id]: $e")
        } finally {
            logger.info("Device[$id] has been disconnected")
            dataStore.clear() // todo: send it throw eventbus system to client socket
            onDisconnect(this@WSLibraryConnection)
        }
    }

    internal suspend fun send(message: WSLibraryMessage) {
        try {
            if (session.isActive) {
                session.send(Frame.Text(json.encodeToString(WSLibraryMessage.serializer(), message)))
            }
        } catch (e: Exception) {
            logger.error("Cannot send message to device[$id]: ${e.message}")
        }
    }

    private suspend fun handleMessage(frame: Frame) {
        if (frame is Frame.Text) {
            val text = frame.readText()
            try {
                val message = json.decodeFromString<WSLibraryMessage>(text)
                logger.info("Message from device[$id] has been received: $text")
                when (message) {
                    is WSLibraryMessage.Command -> {
                        logger.info("Received COMMAND message: ${message.data}")
                    }
                    is WSLibraryMessage.Network -> {
                        logger.info("Received NETWORK message: ${message.data}")
                        try {
                            dataStore.addNetworkRequest(message.data)// todo: send it throw eventbus system to client socket
                            WSClientConnectionBroker.broadcastNetwork(id, message.data)
                        } catch (e: Exception) {
                            logger.error("Failed to parse network data: ${e.message}")
                        }
                    }
                    is WSLibraryMessage.Telemetry -> {
                        logger.info("Received TELEMETRY message: ${message.data}")
                    }
                    is WSLibraryMessage.Logcat -> {
                        logger.info("Received LOGCAT message: ${message.data}")
                        dataStore.addLogcatTrace(message.data)// todo: send it throw eventbus system to client socket
                        WSClientConnectionBroker.broadcastLogcat(id, message.data)
                    }
                    is WSLibraryMessage.Database -> {
                        logger.info("Received DATABASE message: ${message.data}")
                        dataStore.addDatabaseTrace(message.data)// todo: send it throw eventbus system to client socket
                        WSClientConnectionBroker.broadcastDatabase(id, message.data)
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to parse WSLibraryMessage: ${e.message}")
            }
        }
    }
}
