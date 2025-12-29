package com.unde.server.socket.client

import com.unde.server.socket.client.model.WSClientMessage
import com.unde.server.socket.model.UndeRequestResponse
import kotlinx.serialization.json.JsonObject

internal object WSClientConnectionBroker {
    private val connections = mutableMapOf<String, WSClientConnection>()

    fun register(connection: WSClientConnection) {
        synchronized(connections) {
            connections[connection.id] = connection
        }
    }

    fun unregister(connection: WSClientConnection) {
        synchronized(connections) {
            connections.remove(connection.id)
        }
    }

    suspend fun broadcastNetwork(connectionId: String, data: UndeRequestResponse) {
        val message = WSClientMessage.Network(connectionId, data)
        broadcastToAll(message)
    }

    suspend fun broadcastDatabase(connectionId: String, data: JsonObject) {
        val message = WSClientMessage.Database(connectionId, data)
        broadcastToAll(message)
    }

    suspend fun broadcastLogcat(connectionId: String, data: JsonObject) {
        val message = WSClientMessage.Logcat(connectionId, data)
        broadcastToAll(message)
    }

    suspend fun broadcastConnectionsList(connections: List<String>) {
        val message = WSClientMessage.ConnectionsList(connections)
        broadcastToAll(message)
    }

    private suspend fun broadcastToAll(message: WSClientMessage) {
        val connectionsCopy = synchronized(connections) {
            connections.values.toList()
        }
        connectionsCopy.forEach { it.send(message) }
    }
}
