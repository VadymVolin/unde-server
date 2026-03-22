package com.unde.server.socket.remote.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.measureTime

class SessionRegistryTest {

    @Before
    fun setup() {
        SessionRegistry.clear()
    }

    @Test
    fun `test create session stores client id and returns session id`() {
        val clientId = "client-123"
        val sessionId = SessionRegistry.createSession(clientId)

        assertNotNull(sessionId)
        assertEquals(clientId, SessionRegistry.getClientId(sessionId))
    }

    @Test
    fun `test resume session with correct client id succeeds`() {
        val clientId = "client-123"
        val sessionId = SessionRegistry.createSession(clientId)

        val resumed = SessionRegistry.resumeSession(clientId, sessionId)
        assertTrue(resumed)
    }

    @Test
    fun `test resume session with wrong client id fails`() {
        val clientId = "client-123"
        val sessionId = SessionRegistry.createSession(clientId)

        val resumed = SessionRegistry.resumeSession("wrong-client", sessionId)
        assertFalse(resumed)
    }

    @Test
    fun `test remove session clears data`() {
        val clientId = "client-123"
        val sessionId = SessionRegistry.createSession(clientId)

        SessionRegistry.removeSession(sessionId)
        assertNull(SessionRegistry.getClientId(sessionId))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test mark offline and stale session logic`() = runTest {
        Dispatchers.resetMain()
        val clientId = "client-123"
        val sessionId = SessionRegistry.createSession(clientId)
        SessionRegistry.markSessionOffline(sessionId)

        // At threshold 500ms, should not be stale immediately
        var staleSessions = SessionRegistry.getStaleSessions(500L)
        assertTrue(staleSessions.isEmpty())

        val elapsed = TimeSource.Monotonic.measureTime {
            val deferred = async {
                delay(1.seconds) // will be skipped
                withContext(Dispatchers.Default) {
                    delay(500.milliseconds) // Dispatchers.Default doesn't know about TestCoroutineScheduler
                }
            }
            deferred.await()
        }
        println(elapsed) // about five seconds

        staleSessions = SessionRegistry.getStaleSessions(500L)
        assertEquals(1, staleSessions.size)
        assertEquals(sessionId, staleSessions.first())
    }
}
