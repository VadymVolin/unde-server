package com.unde.server.socket.remote

import com.unde.server.constants.JsonToken
import com.unde.server.constants.Text
import com.unde.server.socket.WSDataManager
import com.unde.server.socket.remote.model.SocketRemoteMessage
import com.unde.server.socket.remote.model.createResultCommandData
import io.ktor.server.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

//internal class WSRemoteConnection(
//    val connectionId: String,
//    private val session: DefaultWebSocketServerSession
//)  {
//    private val logger = KtorSimpleLogger(javaClass.simpleName)
//
//    private val json = Json { classDiscriminator = JsonToken.TYPE_TOKEN }
//
//    internal suspend fun connect() {
//        logger.info("The connection to device[$connectionId] has been established")
//        try {
//            WSDataManager.addRemoteConnection(connectionId)
//            send(SocketRemoteMessage.Command(createResultCommandData(Text.CONNECTED_SUCCESSFULLY)))
//            session.incoming.consumeEach(::handleMessage)
//        } catch (e: Exception) {
//            if (e is CancellationException) throw e
//            logger.info("Exception has been caught, device[$connectionId]: $e")
//            WSDataManager.removeRemoteConnection(connectionId)
//        } finally {
//            logger.info("Device[$connectionId] has been disconnected")
//            WSDataManager.removeRemoteConnection(connectionId)
//        }
//    }
//
//    internal suspend fun send(message: SocketRemoteMessage) {
//        try {
//            if (session.isActive) {
//                session.send(Frame.Text(json.encodeToString(SocketRemoteMessage.serializer(), message)))
//            }
//        } catch (e: Exception) {
//            if (e is CancellationException) throw e
//            logger.error("Cannot send message to device[$connectionId]: ${e.message}")
//        }
//    }
//
//    private fun handleMessage(frame: Frame) {
//        if (frame is Frame.Text) {
//            try {
//                val text = frame.readText()
//                val message = json.decodeFromString<SocketRemoteMessage>(text)
//                logger.info("Message from device[$connectionId] has been received: $text")
//                when (message) {
//                    is SocketRemoteMessage.Command -> {
//                        logger.info("Received COMMAND message: ${message.data}")
//                    }
//
//                    is SocketRemoteMessage.Network -> {
//                        logger.info("Received NETWORK message: ${message.data}")
//                        WSDataManager.addNetworkMessage(connectionId, message)
//                    }
//
//                    is SocketRemoteMessage.Telemetry -> {
//                        logger.info("Received TELEMETRY message: ${message.data}")
////                        WSDataManager.addTelemetry(remoteId, message)
//                    }
//
//                    is SocketRemoteMessage.Logcat -> {
//                        logger.info("Received LOGCAT message: ${message.data}")
////                        WSDataManager.addLogcatTrace(remoteId, message)
//                    }
//
//                    is SocketRemoteMessage.Database -> {
//                        logger.info("Received DATABASE message: ${message.data}")
////                        WSDataManager.addDatabaseTrace(remoteId, message)
//                    }
//                }
//            } catch (e: Exception) {
//                logger.error("Failed to parse WSLibraryMessage: ${e.message}")
//            }
//        }
//    }
//}
