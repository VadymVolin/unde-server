package com.unde.server.configuration.router.api

import com.unde.server.configuration.router.api.ping.ping
import com.unde.server.configuration.router.api.ping.test
import io.ktor.server.routing.*

fun Routing.setupHttpRoutingConfiguration() {
    route("/api") {
        ping()
        test()
    }
}