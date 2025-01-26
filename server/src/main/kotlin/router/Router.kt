package com.unde.server.router

import com.unde.server.router.socket.setupWebSocketConfiguration
import io.ktor.server.routing.*

fun Routing.registerRoutes() {
    setupWebSocketConfiguration()
}