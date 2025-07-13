package com.unde.server.configuration.router.socket

import com.unde.server.constants.Route
import io.ktor.server.plugins.origin
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json

fun Routing.setupWebSocketRoutingConfiguration() = webSocket(Route.DEFAULT_WEB_SOCKET_ROUTE) {
    val conn = WebSocketConnection(call.request.origin.remoteAddress, this) {
        ConnectionRegistry.unregister(it)
    }

    ConnectionRegistry.register(conn)
    conn.start() // Start listening
}