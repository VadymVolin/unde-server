package com.unde.server.constants

internal object Route {
    const val DEFAULT_ROOT_URL = "/"
    const val DEFAULT_STATIC_ROUTE = "/"

    // TODO: revert, for testing only, until new lib version 0.0.2
    const val LIBRARY_WEB_SOCKET_ROUTE = "/ws"
    const val CLIENT_WEB_SOCKET_ROUTE = "/ws/client"

    const val EXIT_ROUTE = "/unde/exit"
}