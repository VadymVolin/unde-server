package com.unde.server.socket

import com.unde.server.socket.model.WSConnectionDataStore
import com.unde.server.socket.remote.model.SocketRemoteMessage
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal object WSDataManager {
    private val logger = KtorSimpleLogger(javaClass.simpleName)

    private val _remoteConnections = MutableStateFlow<List<String>>(emptyList())
    val remoteConnections = _remoteConnections.asStateFlow()

    private val _wsRemoteData = MutableStateFlow<Map<String, WSConnectionDataStore>>(emptyMap())

    fun addRemoteConnection(remoteClientId: String) {
        logger.info("New remote connection: $remoteClientId")
        _wsRemoteData.update { it.plus(remoteClientId to WSConnectionDataStore()) }
        _remoteConnections.update { it + remoteClientId }
    }

    fun removeRemoteConnection(remoteClientId: String) {
        logger.info("Remove remote connection: $remoteClientId")
        _remoteConnections.update { it - remoteClientId }
        _wsRemoteData.update {
            it.apply {
                // TODO: FUTURE - Do we need to clear the data when device is disconnected?
                //  What if user wants to see network data later?
                it[remoteClientId]?.clear()
                it - remoteClientId
            }
        }
    }

    fun addNetworkMessage(remoteClientId: String, network: SocketRemoteMessage.Network) = _wsRemoteData.update {
        it.apply {
            it[remoteClientId]?.addNetworkRequest(network.data)
        }
        it.toMap()
    }.also {
        logger.info("New network message has been added.")
    }

    fun getNetworkRequestsByRemoteClientId(remoteClientId: String) = _wsRemoteData.value[remoteClientId]?.networkRequests

    fun getNetworkRequestFlowByRemoteClientId(remoteClientId: String) = _wsRemoteData.value[remoteClientId]?.networkRequestFlow
}