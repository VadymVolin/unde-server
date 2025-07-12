package com.unde.server

import com.unde.server.configuration.adb.AdbManager
import com.unde.server.configuration.plugin.*
import com.unde.server.configuration.plugin.configureSockets
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    AdbManager.setup(this)
    configureLogging()
    configureSerialization()
    configureDatabases()
    configureSockets()
    configureRouting()
}
