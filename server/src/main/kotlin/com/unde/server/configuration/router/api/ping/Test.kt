package com.unde.server.configuration.router.api.ping

import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.test() {
    get("/test") {
        call.respond("Test")
    }
}