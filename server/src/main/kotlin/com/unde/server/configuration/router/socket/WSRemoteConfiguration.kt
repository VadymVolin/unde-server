package com.unde.server.configuration.router.socket

import com.unde.server.constants.Route
import com.unde.server.socket.remote.WSRemoteConnection
import com.unde.server.socket.remote.WSRemoteConnectionBroker
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

internal fun Routing.setupWSRemoteRoutingConfiguration() = webSocket(Route.REMOTE_WEB_SOCKET_ROUTE) {
    val connection = WSRemoteConnection(call.request.origin.remoteAddress, this) {
        WSRemoteConnectionBroker.unregister(it)
    }
    WSRemoteConnectionBroker.register(connection)
    connection.connect()
}