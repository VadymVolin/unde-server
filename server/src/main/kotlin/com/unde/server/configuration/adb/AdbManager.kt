package com.unde.server.configuration.adb

import com.unde.server.constants.Time
import io.ktor.server.application.*
import io.ktor.util.logging.*
import kotlinx.coroutines.*
import java.io.BufferedReader

/**
 * Manages ADB (Android Debug Bridge) operations.
 *
 * This singleton is responsible for:
 * - Detecting connected Android devices.
 * - Setting up reverse port forwarding (`adb reverse`) to allow devices to connect to the server's local port.
 * - Periodically polling for new devices.
 */
object AdbManager {

    private var adbDevicesJob: Job? = null

    private val adbErrors = listOf(
        "adb: no devices/emulators found",
    )

    private val LOGGER = KtorSimpleLogger(javaClass.simpleName)

    /**
     * Initializes the ADB manager and starts the device monitoring job.
     *
     * @param application The Ktor [Application] instance used to launch the coroutine.
     */
    internal fun setup(application: Application) {
        adbDevicesJob?.cancel()
        adbDevicesJob = application.launch(Dispatchers.IO) {
            while (isActive) {
                val connectedDevicesDeferred = application.async { getConnectedDevices() }
                val reversedDevicesDeferred = application.async { getReversedDevices() }
                ensureActive()
                setupAdbReverse(connectedDevicesDeferred.await(), reversedDevicesDeferred.await())
                delay(Time.DEFAULT_ADB_TASK_DELAY)
            }
        }
    }

    /**
     * Stops the device monitoring job and releases resources.
     */
    internal fun release() {
        LOGGER.info("Release manager and resources")
        adbDevicesJob?.cancel()
        adbDevicesJob = null
    }

    private suspend fun getConnectedDevices(): List<String> {
        currentCoroutineContext().ensureActive()
        LOGGER.info("Trying to get connected devices")
        val process = ProcessBuilder("adb", "devices", "-l").redirectErrorStream(true).start()
        currentCoroutineContext().ensureActive()
        val devices = process.inputStream.bufferedReader().useLines { lines ->
            lines.drop(1) // Skip the header "List of devices attached"
                .filter { it.contains("device") && !it.contains("unauthorized") }
                .map { line -> line.split(" ").firstOrNull { it.isNotBlank() }.orEmpty() }
                .filter { it.isNotBlank() }.toList()
        }
        currentCoroutineContext().ensureActive()
        LOGGER.info("Adb devices: $devices")
        process.waitFor()
        process.destroy()
        process.destroyForcibly()
        return devices
    }

    private suspend fun getReversedDevices(): List<String> {
        currentCoroutineContext().ensureActive()
        LOGGER.info("Trying to get reversed devices")
        val process = ProcessBuilder("adb", "reverse", "--list").redirectErrorStream(true).start()
        currentCoroutineContext().ensureActive()
        val devices = process.inputStream.bufferedReader().useLines { lines ->
            lines
                .filter { !it.contains("error:") }
                .map { line -> line.split(" ").firstOrNull { it.isNotBlank() }.orEmpty() }
                .filter { it.isNotBlank() }.toList()
        }
        currentCoroutineContext().ensureActive()
        LOGGER.info("ADB REVERSED DEVICES: $devices")
        process.waitFor()
        process.destroy()
        process.destroyForcibly()
        return devices
    }

    private suspend fun setupAdbReverse(availableDevices: List<String> = emptyList(), reversedDevices: List<String>) {
        val connectedDevices = availableDevices.associateWith { reversedDevices.contains(it) }.toMutableMap()
        try {
            connectedDevices.forEach { deviceToReverse ->
                if (!deviceToReverse.value) {
                    currentCoroutineContext().ensureActive()
                    val process = ProcessBuilder("adb", "-s", deviceToReverse.key, "reverse", "tcp:8081", "tcp:8081")
                        .redirectErrorStream(true)
                        .start()
                    LOGGER.info("Trying to setup adb reverse for device: ${deviceToReverse.key}")
                    currentCoroutineContext().ensureActive()
                    val result = process.inputStream.bufferedReader().use(BufferedReader::readText)
                    currentCoroutineContext().ensureActive()
                    if (adbErrors.contains(result)) {
                        LOGGER.warn("ADB reverse was not installed because of [$result]")
                        throw IllegalStateException("ADB reverse was not installed because of [$result]")
                    }
                    // Wait for the process to finish
                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        connectedDevices[deviceToReverse.key] = true
                    }
                    process.destroy()
                    process.destroyForcibly()
                    LOGGER.info("ADB REVERSE: \n| result: $result \n| exitCode: $exitCode")
                }
            }
        } catch (e: Exception) {
            LOGGER.warn("ADB reverse cannot be configured because of [${e.stackTrace}]")
        }
    }

}