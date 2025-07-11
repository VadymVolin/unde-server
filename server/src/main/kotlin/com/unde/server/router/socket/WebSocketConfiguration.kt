package com.unde.server.router.socket

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json

fun Routing.setupWebSocketRoutingConfiguration() {
    webSocket("/ws") { // websocketSession
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val wsMessage = Json.decodeFromString<String>(frame.readText())
            }
        }
    }
}