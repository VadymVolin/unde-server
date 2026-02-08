package com.unde.server.socket.remote

import com.unde.server.constants.JsonToken
import com.unde.server.socket.WSDataManager
import com.unde.server.socket.remote.model.SocketRemoteMessage
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.logging.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

/**
 * Manages the raw TCP socket server for device connections.
 *
 * This singleton:
 * - Listens on a specific port (default 8081) for incoming TCP connections from Android devices.
 * - Manages the lifecycle of these connections.
 * - Handles incoming raw data streams and deserializes them.
 * - Sends responses back to devices.
 */
internal object ServerSocketConnection {
    private val logger = KtorSimpleLogger(javaClass.simpleName)

    private val json = Json {
        classDiscriminator = JsonToken.TYPE_TOKEN
        ignoreUnknownKeys = true
    }

    private val selectorManager = SelectorManager(Dispatchers.IO)
    private var socketScope: CoroutineScope? = null
    private var serverSocket: ServerSocket? = null
    private val sockets = mutableMapOf<String, Socket>()

    /**
     * Starts the TCP server and listens for incoming connections.
     *
     * @param host The host address to bind to.
     * @param port The port number to listen on.
     */
    internal fun connect(host: String, port: Int) {
        val scope = socketScope ?: CoroutineScope(Dispatchers.IO + SupervisorJob()).also { socketScope = it }
        scope.launch { // server coroutine
            logger.info("Connecting server socket to $host:$port")
            serverSocket = aSocket(selectorManager).tcp().bind(host, port).also { ss ->
                while (isActive) {
                    logger.info("Checking new connection:")
                    var socketId = ""
                    try {
                        with(ss.accept()) {
                            socketId = remoteAddress.toString()
                            sockets[socketId] = this
                            WSDataManager.addRemoteConnection(socketId)
                            logger.info("Accepted connection to client socket: $socketId")
                            listenSocketById(socketId, this)
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        logger.error("Failed to connect to $host:$port", e)
                        disconnectClientById(socketId)
                    }
                }
            }
        }
    }

    /**
     * Stops the TCP server and closes all active connections.
     */
    internal fun disconnect() {
        sockets.keys.forEach {
            disconnectClientById(it)
        }
        sockets.clear()
        try {
            socketScope?.cancel()
            serverSocket?.dispose()
        } catch (_: Exception) {
            logger.error("Cannot release server socket")
        }
    }

    private fun listenSocketById(clientId: String, client: Socket) {
        client.launch {
            val readChannel = client.openReadChannel()
            val writeChannel = client.openWriteChannel(true)
            while (!client.isClosed && !readChannel.isClosedForRead) {
                logger.info("Listen for a new message: ")
                try {
                    val dataSize = readChannel.readUTF8Line()?.toInt() ?: throw SocketDataException()
                    val byteArray = ByteArray(dataSize)
                    ensureActive()
                    readChannel.readFully(byteArray)
                    ensureActive()
                    val data = byteArray.decodeToString()
                    handleReceivedData(clientId, data)
//                    send(client, writeChannel, SocketRemoteMessage.Result(dataSize.toString()))
                } catch (e: Exception) {
                    when (e) {
                        is CancellationException -> throw e
                        is SocketDataException -> {
                            logger.error("Failed to read data from client $clientId, data is null, start disconnection", e)
                            disconnectClientById(clientId)
                        }
                        else -> {
                            logger.error("Failed to read data from client $clientId", e)
                        }
                    }
                }
            }
        }
    }

    private fun send(client: Socket, writeChannel: ByteWriteChannel, message: SocketRemoteMessage) {
        client.launch {
            try {
                val encodedJsonString = json.encodeToString(message)
                writeChannel.writeStringUtf8(encodedJsonString)
                logger.info("Sent message[${message.javaClass.simpleName}]: $encodedJsonString")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.error("Failed to send message: ${e.message}", e)
            }
        }

    }

    private fun handleReceivedData(clientId: String, data: String) {
        try {
            val message = json.decodeFromString<SocketRemoteMessage>(data)
            logger.info("Message from client[$clientId] has been received: $data")
            when (message) {
                is SocketRemoteMessage.Command -> {
                    logger.info("Received COMMAND message: ${message.data}")
                }

                is SocketRemoteMessage.Network -> {
                    logger.info("Received NETWORK message: ${message.data}")
                    WSDataManager.addNetworkMessage(clientId, message)
                }

                is SocketRemoteMessage.Telemetry -> {
                    logger.info("Received TELEMETRY message: ${message.data}")
//                        WSDataManager.addTelemetry(remoteId, message)
                }

                is SocketRemoteMessage.Logcat -> {
                    logger.info("Received LOGCAT message: ${message.data}")
//                        WSDataManager.addLogcatTrace(remoteId, message)
                }

                is SocketRemoteMessage.Database -> {
                    logger.info("Received DATABASE message: ${message.data}")
//                        WSDataManager.addDatabaseTrace(remoteId, message)
                }

                else -> logger.error("Unhandled message: ${message.javaClass.simpleName} - $message")
            }
        } catch (e: Exception) {
            logger.error("Failed to parse message: ", e)
        }
    }

    private fun disconnectClientById(socketId: String) {
        with(sockets.remove(socketId) ?: return) {
            try {
                WSDataManager.removeRemoteConnection(socketId)
                cancel()
                dispose()
                logger.info("Client socket[$socketId] disconnected")
            } catch (e: Exception) {
                logger.error("Cannot disconnect socket[$socketId]", e)
            }
        }
    }

    private class SocketDataException : IllegalArgumentException("Cannot read message, data is null")
}
