package com.unde.server.configuration.router.api.ping

import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.ping() {
    get("/ping") { // HTTP endpoint to test connection
        call.respondText("Pong")
    }
}