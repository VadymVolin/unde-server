package com.unde.server.configuration.router

import com.unde.server.configuration.router.api.setupHttpRoutingConfiguration
import com.unde.server.configuration.router.socket.setupWSRemoteRoutingConfiguration
import com.unde.server.configuration.router.socket.setupWSLocalRoutingConfiguration
import io.ktor.server.routing.*

internal fun Routing.registerRoutes() {
    setupHttpRoutingConfiguration()
    setupWSRemoteRoutingConfiguration()
    setupWSLocalRoutingConfiguration()
}