package com.unde.server

import com.unde.server.configuration.adb.AdbManager
import com.unde.server.configuration.plugin.*
import com.unde.server.configuration.plugin.configureSockets
import io.ktor.server.application.*
import io.ktor.server.netty.*

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
fun Application.module() {
    AdbManager.setup(this)
    configureLogging()
    configureSerialization()
    configureDatabases()
    configureSockets()
    configureRouting()
}
