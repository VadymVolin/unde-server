package com.unde.server.configuration.adb

import io.ktor.util.logging.*
import java.io.BufferedReader
import java.util.*
import kotlin.concurrent.timer

object AdbManager {

    private const val RIGHT_NOW_TIME_MS = 0L
    private const val NEXT_ATTEMPT_DELAY_MS = 5000L

    private const val TIMER_NAME = "ADB manager task scheduler"

    private lateinit var timer: Timer

    private val LOGGER = KtorSimpleLogger("com.unde.server.configuration.adb.AdbManager")

    private val adbErrors = listOf(
        "adb: no devices/emulators found",
    )

    private fun createAdbTask() = try {
        val devices = getConnectedDevices()
        if (devices.isEmpty()) throw IllegalStateException("No devices found")
        setupAdbReverse(devices)
    } catch (e: Exception) {
        LOGGER.warn("Error setting up ADB reverse: ${e.message}")
    }

    private fun getConnectedDevices(): List<String> {
        LOGGER.info("Trying to get connected devices")
        val process = ProcessBuilder("adb", "devices").redirectErrorStream(true).start()

        val devices = process.inputStream.bufferedReader().useLines { lines ->
            lines.drop(1) // Skip the header "List of devices attached"
                .filter { it.contains("device") && !it.contains("unauthorized") }
                .map { line ->
                    return@map line.split(" ").firstOrNull { it.isNotBlank() } ?: ""
                }.filter { it.isNotBlank() }.toList()
        }
        LOGGER.info("Adb devices: $devices")
        process.waitFor()
        return devices
    }

    private fun setupAdbReverse(devices: List<String> = emptyList()) {
        devices.forEach {
            val process = ProcessBuilder("adb", "-s", it, "reverse", "tcp:8080", "tcp:8080")
                .redirectErrorStream(true)
                .start()
            LOGGER.info("Trying to setup adb reverse for device: $it")
            val result = process.inputStream.bufferedReader().use(BufferedReader::readText)
            if (adbErrors.contains(result)) {
                LOGGER.warn("ADB reverse was not installed because of [$result]")
                throw IllegalStateException("ADB reverse was not installed because of [$result]")
            }
            // Wait for the process to finish
            val exitCode = process.waitFor()
            LOGGER.info("ADB Reverse result: $result | $exitCode")
        }
    }

    fun setup() {
        LOGGER.info("Setup Adb manager")
        timer = timer(TIMER_NAME, true, RIGHT_NOW_TIME_MS, NEXT_ATTEMPT_DELAY_MS) { createAdbTask() }
    }

    fun stop() = timer.cancel()

}