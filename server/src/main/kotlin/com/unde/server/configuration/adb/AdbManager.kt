package com.unde.server.configuration.adb

import java.io.BufferedReader

object AdbManager {

    fun setupAdbReverse() {
        try {
            val process = ProcessBuilder("adb", "reverse", "tcp:8080", "tcp:8080")
                .redirectErrorStream(true)
                .start()
            val result = process.inputStream.bufferedReader().use(BufferedReader::readText)
            if (adbErrors.contains(result)) {
                throw IllegalStateException("ADB reverse was not installed because of [$result]")
            }
            // Wait for the process to finish
            val exitCode = process.waitFor()
            println("ADB Reverse result: $result | $exitCode")
        } catch (e: Exception) {
            println("Error setting up ADB reverse: ${e.message}")
        }
    }

    private val adbErrors = listOf(
        "adb: no devices/emulators found",
    )

}