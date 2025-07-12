package com.unde.server.configuration.router

import com.unde.server.configuration.router.api.setupHttpRoutingConfiguration
import com.unde.server.configuration.router.socket.setupWebSocketRoutingConfiguration
import io.ktor.server.routing.*

fun Routing.registerRoutes() {
    setupHttpRoutingConfiguration()
    setupWebSocketRoutingConfiguration()
}