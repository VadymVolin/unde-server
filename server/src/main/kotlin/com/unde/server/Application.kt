package com.unde.server

import com.unde.server.configuration.adb.AdbManager
import com.unde.server.configuration.plugin.*
import com.unde.server.socket.remote.session.SessionCleanupManager
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

/**
 * Application entry point.
 */
fun main(args: Array<String>) {
    EngineMain.main(args)
}

/**
 * Ktor module configuration.
 *
 * Configures all server plugins and features:
 * - ADB Manager setup
 * - Logging
 * - Serialization
 * - Database support
 * - Sockets/WebSockets
 * - Routing
 */
@OptIn(DelicateCoroutinesApi::class)
fun Application.module() {
    AdbManager.setup(this)
    configureLogging()
    configureSerialization()
    configureDatabases()
    configureSockets()
    configureRouting()
    
    SessionCleanupManager.start(GlobalScope)
}
