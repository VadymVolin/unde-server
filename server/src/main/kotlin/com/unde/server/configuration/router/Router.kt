package com.unde.server.configuration.router

import com.unde.server.configuration.router.api.setupHttpRoutingConfiguration
import com.unde.server.configuration.router.socket.setupWSLibraryRoutingConfiguration
import com.unde.server.configuration.router.socket.setupWSClientRoutingConfiguration
import io.ktor.server.routing.*

internal fun Routing.registerRoutes() {
    setupHttpRoutingConfiguration()
    setupWSLibraryRoutingConfiguration()
    setupWSClientRoutingConfiguration()
}