package com.unde.server.configuration.router.socket

import com.unde.server.constants.Route
import com.unde.server.socket.client.WSClientConnection
import com.unde.server.socket.client.WSClientConnectionBroker
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

internal fun Routing.setupWSClientRoutingConfiguration() = webSocket(Route.CLIENT_WEB_SOCKET_ROUTE) {
    val connection = WSClientConnection(session = this) {
        WSClientConnectionBroker.unregister(it)
    }
    WSClientConnectionBroker.register(connection)
    connection.connect()
}
