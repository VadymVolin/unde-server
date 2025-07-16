package com.unde.server.configuration.router.socket

import com.unde.server.constants.Route
import com.unde.server.socket.WSConnection
import com.unde.server.socket.WSConnectionBroker
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

internal fun Routing.setupWebSocketRoutingConfiguration() = webSocket(Route.DEFAULT_WEB_SOCKET_ROUTE) {
    val connection = WSConnection(call.request.origin.remoteAddress, this) {
        WSConnectionBroker.unregister(it)
    }
    WSConnectionBroker.register(connection)
    connection.connect()
}