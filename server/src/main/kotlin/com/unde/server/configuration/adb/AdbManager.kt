package com.unde.server.configuration.adb

import com.unde.server.constants.Time
import io.ktor.server.application.*
import io.ktor.util.logging.*
import kotlinx.coroutines.*
import java.io.BufferedReader

object AdbManager {

    private var adbDevicesJob: Job? = null

    private val adbErrors = listOf(
        "adb: no devices/emulators found",
    )

    private val LOGGER = KtorSimpleLogger(javaClass.simpleName)

    internal fun setup(application: Application) {
        adbDevicesJob?.cancel()
        adbDevicesJob = application.launch(Dispatchers.Default) {
            while (isActive) {
                val devicesDeferred = application.async(Dispatchers.Default) { getConnectedDevices() }
                with(devicesDeferred.await()) {
                    if (isNotEmpty()) {
                        setupAdbReverse(this)
                    }
                }
                delay(Time.DEFAULT_ADB_TASK_DELAY)
            }
        }
    }

    internal fun release() {
        LOGGER.info("Release manager and resources")
        adbDevicesJob?.cancel()
        adbDevicesJob = null
    }

    private fun getConnectedDevices(): List<String> {
        LOGGER.info("Trying to get connected devices")
        val process = ProcessBuilder("adb", "devices", "-l").redirectErrorStream(true).start()
        process.inputStream.bufferedReader().useLines { lines ->
            val devices = process.inputStream.bufferedReader().useLines { lines ->
                lines.drop(1) // Skip the header "List of devices attached"
                    .filter { it.contains("device") && !it.contains("unauthorized") && !it.contains("emulator") }
                    .map { line ->
                        return@map line.split(" ").firstOrNull { it.isNotBlank() }.orEmpty()
                    }.filter { it.isNotBlank() }.toList()
            }
            LOGGER.info("Adb devices: $devices")
            process.waitFor()
            return devices
        }
    }

    private fun setupAdbReverse(devices: List<String> = emptyList()) {
        try {
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
                LOGGER.info("ADB Reverse result: $result | exitCode: $exitCode")
            }
        } catch (e: Exception) {
            LOGGER.warn("ADB reverse cannot be configured because of [${e.stackTrace}]")
        }
    }

}