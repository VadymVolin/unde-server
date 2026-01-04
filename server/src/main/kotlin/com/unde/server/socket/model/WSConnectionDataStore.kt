package com.unde.server.socket.model

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update

internal class WSConnectionDataStore() {
    private val networkRequestsStateFlow = MutableStateFlow<List<UndeRequestResponse>>(mutableListOf())

    private val _networkRequestFlow = MutableSharedFlow<UndeRequestResponse>(extraBufferCapacity = 1)
    val networkRequestFlow = _networkRequestFlow.asSharedFlow()

    fun addNetworkRequest(data: UndeRequestResponse) = data.let { networkData ->
        _networkRequestFlow.tryEmit(networkData)
        networkRequestsStateFlow.update { it + networkData }
    }

    val networkRequests: List<UndeRequestResponse>
        get() = networkRequestsStateFlow.value.toList()

    fun clear() {
        networkRequestsStateFlow.update { it.apply { clear() } }
    }
}