package com.unde.server

import com.unde.server.configuration.adb.AdbManager
import com.unde.server.configuration.plugin.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    AdbManager.setup()
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureLogging()
    configureSerialization()
    configureDatabases()
    configureSockets()
    configureRouting()
}
