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

    private var adbJob: Job? = null

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
        adbJob?.cancel()
        adbJob = application.launch(Dispatchers.IO) {
            while (isActive) {
                val connectedDevicesDeferred = application.async { getConnectedDevices() }
                ensureActive()
                setupAdbReverse(connectedDevicesDeferred.await())
                delay(Time.DEFAULT_ADB_TASK_DELAY)
            }
        }
    }

    /**
     * Stops the device monitoring job and releases resources.
     */
    internal fun release() {
        LOGGER.info("Release manager and resources")
        adbJob?.cancel()
        adbJob = null
    }

    private suspend fun getConnectedDevices(): List<String> = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        LOGGER.info("Trying to get connected devices")
        val process = ProcessBuilder("adb", "devices", "-l").redirectErrorStream(true).start()
        currentCoroutineContext().ensureActive()
        val devices = process.inputStream.bufferedReader().useLines { lines ->
            lines.drop(1) // Skip the header "List of devices attached"
                .filter { it.contains("device") && !it.contains("unauthorized") && !it.contains("offline") }
                .map { line -> line.split(" ").firstOrNull { it.isNotBlank() }.orEmpty() }
                .filter { it.isNotBlank() }.toList()
        }
        currentCoroutineContext().ensureActive()
        LOGGER.info("Adb devices: $devices")
        process.waitFor()
        process.destroy()
        process.destroyForcibly()
        return@withContext devices
    }

    private suspend fun setupAdbReverse(availableDevices: List<String> = emptyList()) = withContext(Dispatchers.IO) {
        try {
            availableDevices.forEach { deviceToReverse ->
                currentCoroutineContext().ensureActive()
                val process = ProcessBuilder("adb", "-s", deviceToReverse, "reverse", "tcp:8081", "tcp:8081")
                    .redirectErrorStream(true)
                    .start()
                LOGGER.info("Trying to setup adb reverse for device: $deviceToReverse")
                currentCoroutineContext().ensureActive()
                val result = process.inputStream.bufferedReader().use(BufferedReader::readText)
                currentCoroutineContext().ensureActive()
                if (adbErrors.contains(result)) {
                    LOGGER.warn("ADB reverse was not installed because of [$result]")
                    throw IllegalStateException("ADB reverse was not installed because of [$result]")
                }
                // Wait for the process to finish
                val exitCode = process.waitFor()
                process.destroy()
                process.destroyForcibly()
                LOGGER.info("ADB REVERSE result: $result | exitCode: $exitCode")
            }
        } catch (e: Exception) {
            LOGGER.warn("ADB reverse cannot be configured because of [${e.stackTrace}]")
        }
    }

}