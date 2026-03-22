package com.unde.server.constants

/**
 * Socket configuration constants.
 */
internal object SocketConstants {
    internal const val DEFAULT_SERVER_HOST = "127.0.0.1"
    internal const val DEFAULT_SERVER_SOCKET_PORT = 8081
    
    internal const val DEFAULT_CLEANUP_INTERVAL_MS = 30L * 60 * 1000 // 30 minutes
    internal const val DEFAULT_STALE_SESSION_THRESHOLD_MS = 30L * 60 * 1000 // 30 minutes
}