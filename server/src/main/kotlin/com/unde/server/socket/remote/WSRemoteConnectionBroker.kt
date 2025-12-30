package com.unde.server.socket.remote

import com.unde.server.socket.remote.model.WSRemoteMessage

internal object WSRemoteConnectionBroker {
    private val _connections = mutableMapOf<String, WSRemoteConnection>()

    internal fun register(connection: WSRemoteConnection) {
        _connections[connection.id] = connection
    }

    internal fun unregister(connection: WSRemoteConnection) {
        _connections.remove(connection.id)
    }

    
    internal fun getConnection(id: String): WSRemoteConnection? = _connections[id]
    
    internal fun getActiveConnections(): List<String> = _connections.keys.toList()

    internal suspend fun sendAll(message: WSRemoteMessage) = _connections.values.forEach {
        it.send(message)
    }

    internal suspend fun send(id: String, message: WSRemoteMessage) = _connections[id]?.send(message)

    internal fun release() = _connections.clear()
}