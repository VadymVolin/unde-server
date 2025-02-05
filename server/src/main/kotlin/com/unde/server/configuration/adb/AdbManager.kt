package com.unde.server.configuration.adb

import java.io.BufferedReader
import java.util.*

object AdbManager {

    val timer = Timer()

    fun setup() {
        val devices = getConnectedDevices()
        if (devices.isNotEmpty()) {
            setupAdbReverse(devices)
        }
    }

    fun getConnectedDevices(): List<String> {
        val process = ProcessBuilder("adb", "devices").redirectErrorStream(true).start()
        val devices = mutableListOf<String>()

        process.inputStream.bufferedReader().useLines { lines ->
            lines.drop(1) // Skip the header "List of devices attached"
                .filter { it.contains("device") && !it.contains("unauthorized") }
                .forEach { line ->
                    println(line.split(" "))
                    val deviceName = line.split(" ").filter { it.isNotBlank() }.firstOrNull()
                    if (deviceName != null) {
                        devices.add(deviceName)
                    }
                }
        }
        process.waitFor()
        return devices
    }

    fun setupAdbReverse(devices: List<String> = emptyList()) {
        try {
            devices.forEach {
                val process = ProcessBuilder("adb", "-s", it, "reverse", "tcp:8080", "tcp:8080").redirectErrorStream(true).start()
                val result = process.inputStream.bufferedReader().use(BufferedReader::readText)
                if (adbErrors.contains(result)) {
                    throw IllegalStateException("ADB reverse was not installed because of [$result]")
                }
                // Wait for the process to finish
                val exitCode = process.waitFor()
                println("ADB Reverse result: $result | $exitCode")
            }
        } catch (e: Exception) {
            println("Error setting up ADB reverse: ${e.message}")
        }
    }

    private val adbErrors = listOf(
        "adb: no devices/emulators found",
    )

}