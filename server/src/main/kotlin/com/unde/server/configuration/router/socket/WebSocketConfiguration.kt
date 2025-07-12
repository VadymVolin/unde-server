package com.unde.server.configuration.router.socket

import com.unde.server.constants.Route
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json

fun Routing.setupWebSocketRoutingConfiguration() = webSocket(Route.DEFAULT_WEB_SOCKET_ROUTE) {
    // websocketSession
    for (frame in incoming) {
        if (frame is Frame.Text) {
            val wsMessage = Json.decodeFromString<String>(frame.readText())
        }
    }
}