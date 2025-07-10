package com.unde.server.configuration.plugin

import com.unde.server.configuration.adb.AdbManager
import com.unde.server.constants.Format
import com.unde.server.constants.Route
import com.unde.server.router.registerRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(ShutDownUrl.ApplicationCallPlugin) {
        // The URL that will be intercepted (you can also use the application.config's ktor.deployment.shutdown.url key)
        shutDownUrl = Route.EXIT_ROUTE // POST request
        // A function that will be executed to get the exit code of the process
        exitCodeSupplier = {
            AdbManager.release()
            println("Shutting down the JVM.")
            0
        } // ApplicationCall.() -> Int
    }
    routing {
        // Static plugin. Try to access `/static/index.html`
        staticResources(Route.DEFAULT_STATIC_ROUTE, "static") {
            extensions(Format.HTML_FORMAT, Format.HTM_FORMAT)
            enableAutoHeadResponse()
            preCompressed(CompressedFileType.GZIP)
        }
        registerRoutes()
    }
}
