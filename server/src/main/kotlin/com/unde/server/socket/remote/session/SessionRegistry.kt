package com.unde.server.socket.remote.session

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory registry that manages socket sessions.
 * Maps `sessionId` -> `clientId` and tracks the offline timestamps
 * of disconnected sessions.
 */
internal object SessionRegistry {
    // Maps sessionId -> clientId
    private val sessions = ConcurrentHashMap<String, String>()
    
    // Maps sessionId -> offline timestamp (in milliseconds)
    private val offlineTimestamps = ConcurrentHashMap<String, Long>()

    /** 
     * Creates a new session, generates a sessionId, and stores the mapping.
     */
    fun createSession(clientId: String): String {
        val sessionId = UUID.randomUUID().toString()
        sessions[sessionId] = clientId
        // Newly created session is considered online, so no offline timestamp.
        offlineTimestamps.remove(sessionId)
        return sessionId
    }

    /** 
     * Resumes an existing session. Returns true if the sessionId exists 
     * and belongs to the given clientId. 
     */
    fun resumeSession(clientId: String, sessionId: String): Boolean {
        if (sessions[sessionId] == clientId) {
            // Client is online again, remove offline timestamp
            offlineTimestamps.remove(sessionId)
            return true
        }
        return false
    }

    /** 
     * Removes a session completely.
     */
    fun removeSession(sessionId: String) {
        sessions.remove(sessionId)
        offlineTimestamps.remove(sessionId)
    }

    /** 
     * Gets the clientId associated with a sessionId.
     */
    fun getClientId(sessionId: String): String? {
        return sessions[sessionId]
    }

    /**
     * Marks a session as offline, recording the current time.
     */
    fun markSessionOffline(sessionId: String) {
        if (sessions.containsKey(sessionId)) {
            offlineTimestamps[sessionId] = System.currentTimeMillis()
        }
    }

    /**
     * Gets sessions that have been offline longer than the provided threshold.
     */
    fun getStaleSessions(thresholdMs: Long): List<String> {
        val now = System.currentTimeMillis()
        return offlineTimestamps.entries
            .filter { (now - it.value) > thresholdMs }
            .map { it.key }
    }
    
    /**
     * Clear all sessions, mostly useful for testing or server reset.
     */
    internal fun clear() {
        sessions.clear()
        offlineTimestamps.clear()
    }
}
