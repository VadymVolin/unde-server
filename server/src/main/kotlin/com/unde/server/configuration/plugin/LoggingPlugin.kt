package com.unde.server.configuration.plugin

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import org.slf4j.event.Level

internal fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.INFO
    }
}
