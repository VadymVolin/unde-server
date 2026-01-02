package com.unde.server.socket.remote

import com.unde.server.constants.JsonToken
import com.unde.server.constants.Text
import com.unde.server.socket.WSDataManager
import com.unde.server.socket.remote.model.WSRemoteMessage
import com.unde.server.socket.remote.model.createResultCommandData
import io.ktor.server.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

internal class WSRemoteConnection(
    val remoteId: String,
    private val session: DefaultWebSocketServerSession,
    private val onDisconnect: (WSRemoteConnection) -> Unit
)  {
    private val logger = KtorSimpleLogger(javaClass.simpleName)

    private val json = Json { classDiscriminator = JsonToken.TYPE_TOKEN }

    internal suspend fun connect() {
        logger.info("The connection to device[$remoteId] has been established")
        try {
            send(WSRemoteMessage.Command(createResultCommandData(Text.CONNECTED_SUCCESSFULLY)))
            session.incoming.consumeEach(::handleMessage)
        } catch (e: Exception) {
            logger.info("Exception has been caught, device[$remoteId]: $e")
        } finally {
            logger.info("Device[$remoteId] has been disconnected")
            WSDataManager.clearByRemoteClientId(remoteId)
            onDisconnect(this)
        }
    }

    internal suspend fun send(message: WSRemoteMessage) {
        try {
            if (session.isActive) {
                session.send(Frame.Text(json.encodeToString(WSRemoteMessage.serializer(), message)))
            }
        } catch (e: Exception) {
            logger.error("Cannot send message to device[$remoteId]: ${e.message}")
        }
    }

    private fun handleMessage(frame: Frame) {
        if (frame is Frame.Text) {
            try {
                val text = frame.readText()
                val message = json.decodeFromString<WSRemoteMessage>(text)
                logger.info("Message from device[$remoteId] has been received: $text")
                when (message) {
                    is WSRemoteMessage.Command -> {
                        logger.info("Received COMMAND message: ${message.data}")
                    }

                    is WSRemoteMessage.Network -> {
                        logger.info("Received NETWORK message: ${message.data}")
                        WSDataManager.addNetworkMessage(remoteId, message)
                    }

                    is WSRemoteMessage.Telemetry -> {
                        logger.info("Received TELEMETRY message: ${message.data}")
//                        WSDataManager.addTelemetry(remoteId, message)
                    }

                    is WSRemoteMessage.Logcat -> {
                        logger.info("Received LOGCAT message: ${message.data}")
//                        WSDataManager.addLogcatTrace(remoteId, message)
                    }

                    is WSRemoteMessage.Database -> {
                        logger.info("Received DATABASE message: ${message.data}")
//                        WSDataManager.addDatabaseTrace(remoteId, message)
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to parse WSLibraryMessage: ${e.message}")
            }
        }
    }
}
