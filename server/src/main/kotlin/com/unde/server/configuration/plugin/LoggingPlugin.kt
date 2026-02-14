package com.unde.server.configuration.plugin

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import org.slf4j.event.Level

/**
 * Configures application logging.
 *
 * Sets up [CallLogging] to log incoming HTTP requests with [Level.INFO].
 */
internal fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.INFO
    }
}
