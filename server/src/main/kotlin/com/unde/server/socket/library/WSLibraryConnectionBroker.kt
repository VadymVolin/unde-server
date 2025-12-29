package com.unde.server.socket.library

import com.unde.server.socket.library.model.WSLibraryMessage

internal object WSLibraryConnectionBroker {
    private val _connections = mutableMapOf<String, WSLibraryConnection>()

    internal fun register(connection: WSLibraryConnection) {
        _connections[connection.id] = connection
    }

    internal fun unregister(connection: WSLibraryConnection) {
        _connections.remove(connection.id)
    }

    
    internal fun getConnection(id: String): WSLibraryConnection? = _connections[id]
    
    internal fun getActiveConnections(): List<String> = _connections.keys.toList()

    internal suspend fun sendAll(message: WSLibraryMessage) = _connections.values.forEach {
        it.send(message)
    }

    internal suspend fun send(id: String, message: WSLibraryMessage) = _connections[id]?.send(message)

    internal fun release() = _connections.clear()
}