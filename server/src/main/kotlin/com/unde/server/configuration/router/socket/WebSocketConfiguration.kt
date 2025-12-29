package com.unde.server.configuration.router.socket

import com.unde.server.constants.Route
import com.unde.server.socket.library.WSLibraryConnection
import com.unde.server.socket.library.WSLibraryConnectionBroker
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

internal fun Routing.setupWebSocketRoutingConfiguration() = webSocket(Route.LIBRARY_WEB_SOCKET_ROUTE) {
    val connection = WSLibraryConnection(call.request.origin.remoteAddress, this) {
        WSLibraryConnectionBroker.unregister(it)
    }
    WSLibraryConnectionBroker.register(connection)
    connection.connect()
}