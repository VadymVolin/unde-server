package com.unde.server.configuration.router.socket

import com.unde.server.constants.Route
import com.unde.server.socket.client.WSLocalConnection
import com.unde.server.socket.client.WSLocalConnectionBroker
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

internal fun Routing.setupWSLocalRoutingConfiguration() = webSocket(Route.LOCAL_WEB_SOCKET_ROUTE) {
    val connection = WSLocalConnection(session = this) {
        WSLocalConnectionBroker.unregister(it)
    }
    WSLocalConnectionBroker.register(connection)
    connection.connect()
}
