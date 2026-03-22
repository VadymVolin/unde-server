package com.unde.server.socket.remote.session

import com.unde.server.constants.SocketConstants
import com.unde.server.socket.WSDataManager
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Periodically sweeps stale sessions.
 */
internal object SessionCleanupManager {
    private val logger = KtorSimpleLogger(javaClass.simpleName)
    private var cleanupJob: Job? = null
    
    fun start(scope: CoroutineScope) {
        if (cleanupJob?.isActive == true) return
        
        cleanupJob = scope.launch(Dispatchers.IO) {
            logger.info("Starting SessionCleanupManager background sweep...")
            while (isActive) {
                delay(SocketConstants.DEFAULT_CLEANUP_INTERVAL_MS)
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

    fun stop() {
        cleanupJob?.cancel()
        cleanupJob = null
    }
}
