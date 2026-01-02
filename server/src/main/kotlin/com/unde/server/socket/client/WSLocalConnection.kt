package com.unde.server.socket.client

import com.unde.server.constants.JsonToken
import com.unde.server.socket.WSDataManager
import com.unde.server.socket.client.model.WSLocalMessage
import com.unde.server.socket.remote.WSRemoteConnectionBroker
import io.ktor.server.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.log

internal class WSLocalConnection(
    val id: String = UUID.randomUUID().toString(),
    private val session: DefaultWebSocketServerSession,
    private val onDisconnect: (WSLocalConnection) -> Unit
) {
    private val logger = KtorSimpleLogger(javaClass.simpleName)
    private val json = Json { classDiscriminator = JsonToken.TYPE_TOKEN }

    private var networkDataStoreJob: Job? = null

    internal suspend fun connect() = withContext(Dispatchers.Default) {
        logger.info("Client UI connection [$id] established")
        try {
            // Send initial connections list
            send(WSLocalMessage.ConnectionsList(WSRemoteConnectionBroker.getActiveConnections())) // change to data store
            // Handle incoming messages
            session.incoming.consumeEach { handleMessage(it) }
        } catch (e: Exception) {
            logger.info("Client UI [$id] exception: ${e.message}")
        } finally {
            logger.info("Client UI [$id] disconnected")
            networkDataStoreJob?.cancel()
            onDisconnect(this@WSLocalConnection)
        }
    }

    internal suspend fun send(message: WSLocalMessage) = withContext(Dispatchers.Default) {
        try {
            if (session.isActive) {
                session.send(Frame.Text(json.encodeToString(WSLocalMessage.serializer(), message)))
            }
        } catch (e: Exception) {
            logger.error("Cannot send message to client UI [$id]: ${e.message}")
        }
    }

    private suspend fun handleMessage(frame: Frame) = withContext(Dispatchers.Default) {
        if (frame is Frame.Text) {
            val text = frame.readText()
            try {
                val message = json.decodeFromString<WSLocalMessage>(text)
                logger.info("Message from client UI [$id]: $text")
                when (message) {
                    is WSLocalMessage.SelectConnection -> {
                        logger.info("Client selected connection: ${message.connectionId}")
                        networkDataStoreJob?.cancel()
                        send(
                            WSLocalMessage.Networks(
                                message.connectionId,
                                WSDataManager.getNetworkRequestsByRemoteClientId(message.connectionId)
                            )
                        )
                        networkDataStoreJob = WSDataManager.getNetworkRequestFlowByRemoteClientId(message.connectionId)
                            .onEach {
                                logger.info("SEND !!!!!!! $it")
                                send(WSLocalMessage.Network(message.connectionId, it))
                            }
                            .catch {
                                logger.error("ERROR !!!!!!! $it", it)
                            }
                            .launchIn(session.plus(Dispatchers.Default))
                        logger.info("IS ACTIVE: " + networkDataStoreJob?.isActive)
                    }
                    else -> {
                        logger.warn("Unexpected message type from client: ${message::class.simpleName}")
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.error("Failed to parse WSClientMessage: ${e.message}")
            }
        }
    }
}