package com.unde.server.socket.model

import kotlinx.serialization.json.JsonObject

internal class WSConnectionDataStore(private val remoteClientId: String) {
    private val networkRequests = mutableListOf<UndeRequestResponse>()
    private val databaseTraces = mutableListOf<JsonObject>()
    private val logcatTraces = mutableListOf<JsonObject>()

    @Synchronized
    fun addNetworkRequest(data: UndeRequestResponse) {
        networkRequests.add(data)
    }

    @Synchronized
    fun getNetworkRequests(): List<UndeRequestResponse> = networkRequests.toList()

    @Synchronized
    fun clear() {
        networkRequests.clear()
        databaseTraces.clear()
        logcatTraces.clear()
    }
}