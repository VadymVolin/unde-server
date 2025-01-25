package com.unde.server

import com.unde.server.plugin.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureLogging()
    configureSerialization()
    configureDatabases()
    configureSockets()
    configureRouting()
}
