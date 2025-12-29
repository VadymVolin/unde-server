package com.unde.server.socket.client.data

import com.unde.server.socket.model.UndeRequestResponse
import kotlinx.serialization.json.JsonObject

internal class ConnectionDataStore {
    private val networkRequests = mutableListOf<UndeRequestResponse>()
    private val databaseTraces = mutableListOf<JsonObject>()
    private val logcatTraces = mutableListOf<JsonObject>()

    @Synchronized
    fun addNetworkRequest(data: UndeRequestResponse) {
        networkRequests.add(data)
    }

    @Synchronized
    fun addDatabaseTrace(data: JsonObject) {
        databaseTraces.add(data)
    }

    @Synchronized
    fun addLogcatTrace(data: JsonObject) {
        logcatTraces.add(data)
    }

    @Synchronized
    fun getNetworkRequests(): List<UndeRequestResponse> = networkRequests.toList()

    @Synchronized
    fun getDatabaseTraces(): List<JsonObject> = databaseTraces.toList()

    @Synchronized
    fun getLogcatTraces(): List<JsonObject> = logcatTraces.toList()

    @Synchronized
    fun clear() {
        networkRequests.clear()
        databaseTraces.clear()
        logcatTraces.clear()
    }
}
