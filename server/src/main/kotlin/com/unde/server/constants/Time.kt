package com.unde.server.constants

import kotlin.time.Duration.Companion.seconds

object Time {
    val DEFAULT_ADB_TASK_DELAY = 10L.seconds.inWholeMilliseconds
    const val INITIAL_ADB_TASK_DELAY = 0L
}