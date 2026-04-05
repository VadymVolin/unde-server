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
    
    // Maps sessionId -> offline timestamp (in milliseconds)
    private val offlineTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()
    
    fun markOffline(sessionId: String) {
        offlineTimestamps[sessionId] = System.currentTimeMillis()
    }
    
    fun markOnline(sessionId: String) {
        offlineTimestamps.remove(sessionId)
    }

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
        val now = System.currentTimeMillis()
        val staleSessions = offlineTimestamps.entries
            .filter { (now - it.value) > SocketConstants.DEFAULT_STALE_SESSION_THRESHOLD_MS }
            .map { it.key }
            
        if (staleSessions.isNotEmpty()) {
            logger.info("Sweeping ${staleSessions.size} stale session(s).")
            for (sessionId in staleSessions) {
                offlineTimestamps.remove(sessionId)
                WSDataManager.removeRemoteConnection(sessionId)
            }
        }
    }

    internal fun stop() {
        offlineTimestamps.clear()
        cleanupJob?.cancel()
        cleanupJob = null
    }
}
