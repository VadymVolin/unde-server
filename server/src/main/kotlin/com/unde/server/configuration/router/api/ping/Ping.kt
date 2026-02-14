package com.unde.server.configuration.router.api.ping

import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines the `/ping` endpoint.
 *
 * Used for health checks to verify the server is running.
 */
internal fun Route.ping() {
    get("/ping") { // HTTP endpoint to test connection
        call.respondText("Pong")
    }
}