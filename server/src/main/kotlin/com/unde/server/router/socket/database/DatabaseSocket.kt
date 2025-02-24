package com.unde.server.router.socket.database

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

fun Routing.databaseSocket() {
    webSocket("/ws/database") { // websocketSession
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                outgoing.send(Frame.Text("YOU SAID TO DATABASE: $text"))
                if (text.equals("bye", ignoreCase = true)) {
                    close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                }
            }
        }
    }
}