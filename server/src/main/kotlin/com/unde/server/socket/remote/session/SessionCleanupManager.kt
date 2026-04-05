package com.unde.server.socket.remote.session

import com.unde.server.constants.SocketConstants
import com.unde.server.socket.WSDataManager
import io.ktor.util.logging.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Periodically sweeps stale sessions.
 */
internal object SessionCleanupManager {
    private val logger = KtorSimpleLogger(javaClass.simpleName)
    private var cleanupJob: Job? = null

    internal fun start(scope: CoroutineScope) {
        if (cleanupJob?.isActive == true) return

        cleanupJob = scope.launch(Dispatchers.IO) {
            logger.info("Starting SessionCleanupManager background sweep...")
            while (isActive) {
                delay(SocketConstants.DEFAULT_CLEANUP_INTERVAL_MS.milliseconds)
                sweep()
            }
        }
    }

    internal fun sweep() {
        val staleSessions = SessionRegistry.getStaleSessions(SocketConstants.DEFAULT_STALE_SESSION_THRESHOLD_MS)
        if (staleSessions.isNotEmpty()) {
            logger.info("Sweeping ${staleSessions.size} stale session(s).")
            for (sessionId in staleSessions) {
                SessionRegistry.removeSession(sessionId)
                WSDataManager.removeRemoteConnection(sessionId)
            }
        }
    }

    internal fun stop() {
        SessionRegistry.clear()
        cleanupJob?.cancel()
        cleanupJob = null
    }
}
