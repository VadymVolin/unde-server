package com.unde.server.socket

import com.unde.server.socket.model.WSConnectionDataStore
import com.unde.server.socket.remote.model.WSRemoteMessage
import kotlinx.coroutines.flow.*

internal object WSDataManager {

    private val wsRemoteData = mutableMapOf<String, WSConnectionDataStore>()

    fun getNetworkRequestsByRemoteClientId(remoteClientId: String) = wsRemoteData[remoteClientId]?.getNetworkRequests()
        ?: WSConnectionDataStore().also { wsRemoteData[remoteClientId] = it }.getNetworkRequests()

    fun getNetworkRequestFlowByRemoteClientId(remoteClientId: String) = wsRemoteData[remoteClientId]?.networkRequestFlow
        ?: WSConnectionDataStore().also { wsRemoteData[remoteClientId] = it }.networkRequestFlow

    fun addNetworkMessage(remoteClientId: String, network: WSRemoteMessage.Network) =
        wsRemoteData[remoteClientId]?.addNetworkRequest(network.data)
            ?: WSConnectionDataStore().apply { addNetworkRequest(network.data) }
                .also { wsRemoteData[remoteClientId] = it }

    fun clearByRemoteClientId(remoteClientId: String) = wsRemoteData[remoteClientId]?.clear()

}