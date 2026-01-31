package com.unde.server.configuration.plugin

import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * Configures WebSocket support.
 *
 * Sets up:
 * - JSON serialization for WebSockets using [KotlinxWebsocketSerializationConverter].
 * - Ping/Pong intervals and timeouts.
 */
internal fun Application.configureSockets() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        pingPeriod = 3.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
    }
}
