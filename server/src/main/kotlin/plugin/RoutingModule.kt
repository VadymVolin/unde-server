package com.unde.server.plugin

import com.unde.server.constants.Route
import com.unde.server.route.registerRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.webjars.*

fun Application.configureRouting() {
    install(Resources)
    install(Webjars) {
        path = Route.DEFAULT_WEBJARS_ROUTE //defaults to /webjars
    }
    install(ShutDownUrl.ApplicationCallPlugin) {
        // The URL that will be intercepted (you can also use the application.conf's ktor.deployment.shutdown.url key)
        shutDownUrl = Route.EXIT_ROUTE
        // A function that will be executed to get the exit code of the process
        exitCodeSupplier = { 0 } // ApplicationCall.() -> Int
    }
    routing {
        // Static plugin. Try to access `/static/index.html`
        staticResources(Route.DEFAULT_STATIC_ROUTE, "static")
        get(Route.DEFAULT_WEBJARS_ROUTE) {
            call.respondText("<script src='/webjars/jquery/jquery.js'></script>", ContentType.Text.Html)
        }
        staticResources(Route.DEFAULT_ROOT_URL, Route.DEFAULT_WEB_ROUTE)

        registerRoutes()
    }
}
