package com.unde.server.socket.client

import com.unde.server.constants.JsonToken
import com.unde.server.socket.WSDataManager
import com.unde.server.socket.client.model.WSLocalMessage
import io.ktor.server.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

internal class WSLocalConnection(
    val connectionId: String = UUID.randomUUID().toString(),
    private val session: DefaultWebSocketServerSession
) {
    private val logger = KtorSimpleLogger(javaClass.simpleName)
    private val json = Json { classDiscriminator = JsonToken.TYPE_TOKEN }

    private var networkDataStoreJob: Job? = null
    private var connectionsDataStoreJob: Job? = null

    internal suspend fun connect() = withContext(Dispatchers.Default) {
        logger.info("Client connection [$connectionId] has been established")
        try {
            connectionsDataStoreJob?.cancel()
            networkDataStoreJob?.cancel()
            observeRemoteConnections()
            // Handle incoming messages
            session.incoming.consumeEach { handleMessage(it) }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.info("Exception has been caught, Client [$connectionId],  exception: ${e.message}")
        } finally {
            logger.info("Client [$connectionId] has been disconnected")
            connectionsDataStoreJob?.cancel()
            networkDataStoreJob?.cancel()
        }
    }

    internal suspend fun send(message: WSLocalMessage) = withContext(Dispatchers.Default) {
        try {
            if (session.isActive) {
                session.send(Frame.Text(json.encodeToString(WSLocalMessage.serializer(), message)))
            }
        } catch (e: Exception) {
            logger.error("Cannot send message to client UI [$connectionId]: ${e.message}")
        }
    }

    private suspend fun handleMessage(frame: Frame) = withContext(Dispatchers.Default) {
        if (frame is Frame.Text) {
            val text = frame.readText()
            val message = try {
                logger.info("Message from local client[$connectionId]: $text")
                json.decodeFromString<WSLocalMessage>(text)
            } catch (e: Exception) {
                logger.error("Failed to parse WSClientMessage: ${e.message}")
                null
            }
            when (message) {
                is WSLocalMessage.SelectConnection -> {
                    logger.info("Client selected connection: ${message.connectionId}")
                    networkDataStoreJob?.cancel()
                    WSDataManager.getNetworkRequestsByRemoteClientId(message.connectionId)?.let {
                        send(WSLocalMessage.Networks(message.connectionId, it))
                    }
                    observeNewRequests(message.connectionId)
                }

                else -> {
                    logger.warn("Unexpected message type from local client: ${message?.javaClass?.simpleName}")
                }
            }
        }
    }

    private fun CoroutineScope.observeRemoteConnections() {
        connectionsDataStoreJob = WSDataManager.remoteConnections
            .onEach {
                logger.info("Obtained new connections: $it")
                send(WSLocalMessage.ConnectionsList(it)) // change to data store
            }.catch {
                logger.info("Cannot obtain new connections: $it")
            }.launchIn(this)
    }

    private fun CoroutineScope.observeNewRequests(remoteClientId: String) {
        networkDataStoreJob = WSDataManager.getNetworkRequestFlowByRemoteClientId(remoteClientId)
            ?.onEach {
                logger.info("Obtained new network request from [$remoteClientId]")
                send(WSLocalMessage.Network(remoteClientId, it))
            }?.catch {
                logger.info("Cannot obtain new network request: $it")
            }?.launchIn(this)
    }
}