package com.unde.server.configuration.adb

import com.unde.server.constants.Time
import io.ktor.server.application.*
import io.ktor.util.logging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.util.*

object AdbManager {

    private var timer: Timer? = null

    private var timerTask: TimerTask? = null

    private val adbErrors = listOf(
        "adb: no devices/emulators found",
    )

    private val LOGGER = KtorSimpleLogger(javaClass.simpleName)

    internal fun setup(application: Application) {
        if (timer == null) {
            timer = Timer("UndeTimer")
        }
        if (timerTask == null) {
            timerTask = createAdbDevicesTask(application)
        }
        timer?.scheduleAtFixedRate(timerTask, Time.INITIAL_ADB_TASK_DELAY, Time.DEFAULT_ADB_TASK_DELAY)
    }

    internal fun release() {
        LOGGER.info("Release manager and resources")
        timer?.cancel()
        timer = null
    }

    private fun createAdbDevicesTask(application: Application): TimerTask = object : TimerTask() {
        override fun run() {
            val devicesDeferred = application.async(Dispatchers.Default) { getConnectedDevices() }
            application.launch(Dispatchers.Default) {
                val devices = devicesDeferred.await()
                if (devices.isNotEmpty()) {
                    setupAdbReverse(devices)
                }
            }
        }
    }

    private fun getConnectedDevices(): List<String> {
        LOGGER.info("Trying to get connected devices")
        val process = ProcessBuilder("adb", "devices").redirectErrorStream(true).start()
        process.inputStream.bufferedReader().useLines { lines ->
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