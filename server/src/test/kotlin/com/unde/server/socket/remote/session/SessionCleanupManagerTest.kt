package com.unde.server.socket.remote.session

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionCleanupManagerTest {

    @Before
    fun setup() {
        SessionRegistry.clear()
    }

    @Test
    fun `test sweep removes stale sessions and keeps active ones`() = runTest {
        val activeClientId = "active-client"
        val staleClientId = "stale-client"

        val activeSessionId = SessionRegistry.createSession(activeClientId)
        val staleSessionId = SessionRegistry.createSession(staleClientId)

        // Mark only one as offline
        SessionRegistry.markSessionOffline(staleSessionId)

        // Use reflection to fake the offline timestamp to 31 minutes ago
        // so it exceeds SocketConstants.DEFAULT_STALE_SESSION_THRESHOLD_MS (30 minutes)
        val field = com.unde.server.socket.remote.session.SessionRegistry::class.java.getDeclaredField("offlineTimestamps")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val timestamps = field.get(com.unde.server.socket.remote.session.SessionRegistry) as java.util.concurrent.ConcurrentHashMap<String, Long>
        timestamps[staleSessionId] = System.currentTimeMillis() - (31L * 60 * 1000)

        val staleBeforeSweep = SessionRegistry.getStaleSessions(com.unde.server.constants.SocketConstants.DEFAULT_STALE_SESSION_THRESHOLD_MS)
        assertEquals(1, staleBeforeSweep.size)
        assertEquals(staleSessionId, staleBeforeSweep.first())

        SessionCleanupManager.sweep()

        // Stale session should be removed from registry
        assertNull(SessionRegistry.getClientId(staleSessionId))
        
        // Active session should remain
        assertEquals(activeClientId, SessionRegistry.getClientId(activeSessionId))
    }
}
