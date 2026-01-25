package com.unde.server.configuration.router.socket

import com.unde.server.constants.SocketConstants.DEFAULT_SERVER_SOCKET_PORT
import com.unde.server.socket.remote.ServerSocketConnection
import io.ktor.server.application.*
import io.ktor.server.routing.*

internal fun Routing.setupSocketRemoteRoutingConfiguration() =
    ServerSocketConnection.connect(
        application.engine.environment.config.host,
        DEFAULT_SERVER_SOCKET_PORT
    )