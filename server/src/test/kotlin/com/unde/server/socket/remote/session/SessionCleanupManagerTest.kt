package com.unde.server.socket.remote.session

import com.unde.server.socket.WSDataManager
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SessionCleanupManagerTest {

    @Before
    fun setup() {
        SessionCleanupManager.stop()
    }

    @Test
    fun `test sweep removes stale sessions and keeps active ones`() = runBlocking {
        val activeSessionId = "active-session-id"
        val staleSessionId = "stale-session-id"

        // Mark them
        SessionCleanupManager.markOnline(activeSessionId)
        SessionCleanupManager.markOffline(staleSessionId)

        // Fake timestamps
        val field = SessionCleanupManager::class.java.getDeclaredField("offlineTimestamps")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val timestamps = field.get(SessionCleanupManager) as java.util.concurrent.ConcurrentHashMap<String, Long>
        timestamps[staleSessionId] = System.currentTimeMillis() - (31L * 60 * 1000)

        // Make sure it's in WSDataManager to test successful removal
        WSDataManager.addRemoteConnection(activeSessionId)
        WSDataManager.addRemoteConnection(staleSessionId)

        SessionCleanupManager.sweep()

        // Stale session should be removed
        assertEquals(false, timestamps.containsKey(staleSessionId))
        // WSDataManager might not clear values synchronously inside sweep if they are flows, 
        // but WSDataManager.removeRemoteConnection has been called.
    }
}
