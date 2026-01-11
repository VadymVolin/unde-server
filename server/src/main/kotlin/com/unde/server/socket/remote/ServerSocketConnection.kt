package com.unde.server.socket.remote

import com.unde.server.constants.JsonToken
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.log

internal object ServerSocketConnection {
    private val logger = KtorSimpleLogger(javaClass.simpleName)

    private val json = Json { classDiscriminator = JsonToken.TYPE_TOKEN }

    private val selectorManager = SelectorManager(Dispatchers.IO)
    private var socketScope: CoroutineScope? = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private val sockets = mutableMapOf<String, Socket>()

    internal fun connect(host: String, port: Int) {
        val scope = socketScope ?: CoroutineScope(Dispatchers.IO).also { socketScope = it }
        scope.launch { // server coroutine
            logger.info("Connecting server socket to $host:$port")
            serverSocket = aSocket(selectorManager).tcp().bind(host, port)
            while (isActive) {
                logger.info("Checking connections")
                var socket: Socket? = null
                try {
                    socket = serverSocket?.accept()?.also {
                        logger.info("Accepted connection to client socket: ${it.remoteAddress}")
                        it.launch {
                            // todo: extract it to a separate method and add try catch handling
                            val readChannel = socket?.openReadChannel()
                            val writeChannel = socket?.openWriteChannel(true)
                            val isClosedForRead = !(readChannel?.isClosedForRead ?: true)
                            while (isClosedForRead) {
                                val data = readChannel.readUTF8Line()
                                logger.info("Data received: $data")
                            }
                        }
                        sockets[it.remoteAddress.toString()] = it
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logger.error("Failed to connect to $host:$port", e)
                } finally {
                    val socket = sockets.remove(socket?.remoteAddress?.toString())
                    socket?.cancel()
                    socket?.close()
                    logger.info("Client socket disconnected")
                }
            }
        }
    }

    internal fun disconnect() {
        // todo: release sockets map here
        serverSocket?.close()
        socketScope?.cancel()
    }

//    private fun send(socket, message)

//    private fun handleData(data)


}