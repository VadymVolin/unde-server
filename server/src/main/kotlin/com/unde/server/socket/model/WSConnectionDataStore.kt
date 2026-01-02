package com.unde.server.socket.model

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update

internal class WSConnectionDataStore() {
    private val networkRequestsStateFlow = MutableStateFlow(mutableListOf<UndeRequestResponse>())
    private val _networkRequestFlow = MutableSharedFlow<UndeRequestResponse>(extraBufferCapacity = 1)
    val networkRequestFlow = _networkRequestFlow.asSharedFlow()

    fun addNetworkRequest(data: UndeRequestResponse) = data.let { networkData ->
        val a = _networkRequestFlow.tryEmit(networkData)
        println("NetworkRequestFlow====>>>>> : $a")
        networkRequestsStateFlow.update { it.apply { add(networkData) } }
    }



    @Synchronized
    fun getNetworkRequests(): List<UndeRequestResponse> = networkRequestsStateFlow.value.toList()

    fun clear() {
        networkRequestsStateFlow.update { it.apply { clear() } }
    }
}