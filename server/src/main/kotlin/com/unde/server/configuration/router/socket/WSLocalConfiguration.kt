package com.unde.server.configuration.router.socket

import com.unde.server.constants.Route
import com.unde.server.socket.local.WSLocalConnection
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

internal fun Routing.setupWSLocalRoutingConfiguration() = webSocket(Route.LOCAL_WEB_SOCKET_ROUTE) {
    WSLocalConnection(session = this).connect()
}
