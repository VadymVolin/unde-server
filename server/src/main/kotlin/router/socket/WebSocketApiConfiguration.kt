package com.unde.server.router.socket

import com.unde.server.router.socket.database.databaseSocket
import com.unde.server.router.socket.logcat.logcatSocket
import com.unde.server.router.socket.network.networkSocket
import io.ktor.server.routing.*

fun Routing.setupWebSocketConfiguration() {
    networkSocket()
    logcatSocket()
    databaseSocket()
}