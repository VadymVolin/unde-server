package com.unde.server.configuration.router.api

import com.unde.server.configuration.router.api.ping.ping
import io.ktor.server.routing.*

internal fun Routing.setupHttpRoutingConfiguration() {
    route("/api") {
        ping()
    }
}