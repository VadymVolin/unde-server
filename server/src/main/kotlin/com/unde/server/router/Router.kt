package com.unde.server.router

import com.unde.server.router.api.setupHttpRoutingConfiguration
import com.unde.server.router.socket.setupWebSocketRoutingConfiguration
import io.ktor.server.routing.*

fun Routing.registerRoutes() {
    setupHttpRoutingConfiguration()
    setupWebSocketRoutingConfiguration()
}