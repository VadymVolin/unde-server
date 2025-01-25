package com.unde.server.route

import com.unde.server.router.socket.setupWebSocketConfiguration
import io.ktor.server.routing.*

fun Routing.registerRoutes() {
    setupWebSocketConfiguration()
//    get("/") {
//        call.respondHtml {
//            leaderboardPage(Random(1234124))
//        }
//    }
}