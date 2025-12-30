package com.unde.server.socket.client

import com.unde.server.socket.client.model.WSLocalMessage
import com.unde.server.socket.model.UndeRequestResponse
import kotlinx.serialization.json.JsonObject

internal object WSLocalConnectionBroker {
    private val connections = mutableMapOf<String, WSLocalConnection>()

    fun register(connection: WSLocalConnection) {
        synchronized(connections) {
            connections[connection.id] = connection
        }
    }

    fun unregister(connection: WSLocalConnection) {
        synchronized(connections) {
            connections.remove(connection.id)
        }
    }
}
