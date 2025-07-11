package com.unde.server.router.api

import com.unde.server.router.api.ping.ping
import com.unde.server.router.api.ping.test
import io.ktor.server.routing.*

fun Routing.setupHttpRoutingConfiguration() {
    route("/api") {
        ping()
        test()
    }
}