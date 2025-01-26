package com.unde.server.router.api

import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.setupHttpConfiguration() {
    route("/api") {
        pingApi()
    }
}

fun Routing.pingApi() {
    get("/ping") { // HTTP endpoint to test connection
        call.respondText("Pong")
    }
}