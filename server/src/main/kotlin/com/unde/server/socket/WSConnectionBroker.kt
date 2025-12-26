package com.unde.server.socket

import com.unde.server.socket.model.WSMessage

internal object WSConnectionBroker {
    private val _connections = mutableMapOf<String, WSConnection>()

    internal fun register(connection: WSConnection) {
        _connections[connection.id] = connection
    }

    internal fun unregister(connection: WSConnection) {
        _connections.remove(connection.id)
    }

    internal suspend fun sendAll(message: WSMessage) = _connections.values.forEach {
        it.send(message)
    }

    internal suspend fun send(id: String, message: WSMessage) = _connections[id]?.send(message)

    internal fun release() = _connections.clear()
}