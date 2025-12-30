package com.unde.server.socket

import com.unde.server.socket.model.WSConnectionDataStore
import com.unde.server.socket.remote.model.WSRemoteMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject

internal object WSDataManager {
    
    private val wsRemoteData = MutableStateFlow<Map<String, WSConnectionDataStore>>(mutableMapOf())

    fun getDataByRemoteClientId(remoteClientId: String) = wsRemoteData.map { it[remoteClientId] }.filterNotNull()

    // todo: change to update!!!
    fun addNetworkMessage(remoteClientId: String, message: WSRemoteMessage.Network) =
        wsRemoteData.value[remoteClientId]?.addNetworkRequest(message.data)
        ?: {
            wsRemoteData.value[remoteClientId] = WSConnectionDataStore(remoteClientId).apply { addNetworkMessage(remoteClientId, message.data) }
        }
    
}