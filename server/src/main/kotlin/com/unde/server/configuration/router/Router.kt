package com.unde.server.configuration.router

import com.unde.server.configuration.router.api.setupHttpRoutingConfiguration
import com.unde.server.configuration.router.socket.setupSocketRemoteRoutingConfiguration
import com.unde.server.configuration.router.socket.setupWSLocalRoutingConfiguration
import io.ktor.server.routing.*

/**
 * Registers all application routes.
 *
 * Includes:
 * - HTTP API routes.
 * - Remote Socket routes (for devices).
 * - Local WebSocket routes (for clients).
 */
internal fun Routing.registerRoutes() {
    setupHttpRoutingConfiguration()
    setupSocketRemoteRoutingConfiguration()
    setupWSLocalRoutingConfiguration()
}