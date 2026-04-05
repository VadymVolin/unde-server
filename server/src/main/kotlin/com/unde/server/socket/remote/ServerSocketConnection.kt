package com.unde.server.socket.remote

import com.unde.server.socket.remote.session.SessionCleanupManager

import com.unde.server.constants.JsonToken
import com.unde.server.constants.SocketConstants
import com.unde.server.socket.WSDataManager
import com.unde.server.socket.remote.model.SocketRemoteMessage
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.logging.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import kotlinx.io.asOutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.use

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
    private val sockets = ConcurrentHashMap<String, Socket>()

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
            serverSocket?.close()
            socketScope?.cancel()
        } catch (_: Exception) {
            logger.error("Cannot release server socket")
        }
    }

    private fun listenSocketById(tempId: String, client: Socket) =
        client.launch {
            val readChannel = client.openReadChannel()
            val writeChannel = client.openWriteChannel(true)
            
            var isHandshakeDone = false
            var sessionId = tempId

            while (!client.isClosed && !readChannel.isClosedForRead) {
                logger.info("Listen for a new message: ")
                try {
                    val data = readChannel.readFramedJsonSafe(true)
                    
                    if (!isHandshakeDone) {
                        when (data) {
                            is SocketRemoteMessage.SessionAuth -> {
                                sessionId = data.sessionId
                                isHandshakeDone = true

                                // Check if WSDataManager already knows about this token
                                val isResumed = WSDataManager.hasRemoteConnection(sessionId)
                                
                                // Clean up the temp ID and assign actual Identity
                                sockets.remove(tempId)
                                // If there was a ghost socket holding this token, disconnect it!
                                sockets[sessionId]?.let {
                                    disconnectClientById(sessionId)
                                }
                                sockets[sessionId] = client

                                if (isResumed) {
                                    logger.info("Session resumed for $sessionId")
                                    SessionCleanupManager.markOnline(sessionId)
                                    send(client, writeChannel, SocketRemoteMessage.SessionAck(true))
                                } else {
                                    logger.info("New session started for $sessionId")
                                    WSDataManager.addRemoteConnection(sessionId)
                                    SessionCleanupManager.markOnline(sessionId)
                                    send(client, writeChannel, SocketRemoteMessage.SessionAck(false))
                                }
                            }
                            else -> {
                                logger.error("Protocol violation: first message must be SessionAuth")
                                disconnectClientById(tempId)
                                return@launch
                            }
                        }
                    } else {
                        handleReceivedData(client, writeChannel, sessionId, data)
                    }
                } catch (e: Exception) {
                    when (e) {
                        is CancellationException -> throw e
                        is SocketDataException, is kotlinx.io.EOFException, is ClosedByteChannelException -> {
                            logger.error("Failed to read data from client $sessionId, data is null or channel closed by client, start disconnection", e)
                            disconnectClientById(sessionId)
                        }
                        else -> {
                            logger.error("Failed to read data from client $sessionId", e)
                        }
                    }
                }
            }
        }


    private fun send(client: Socket, writeChannel: ByteWriteChannel, message: SocketRemoteMessage) =
        client.launch {
            try {
                val encodedJsonString = json.encodeToString(SocketRemoteMessage.serializer(), message)
                writeChannel.writeFramedJsonSafe(message, true)
                logger.info("Sent message[${message.javaClass.simpleName}]: $encodedJsonString")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.error("Failed to send message: ${e.message}", e)
            }
        }

    private fun handleReceivedData(
        client: Socket,
        writeChannel: ByteWriteChannel,
        clientId: String,
        message: SocketRemoteMessage
    ) = try {
        logger.info("Message from client[$clientId] has been received: ${message.javaClass.simpleName}")
        when (message) {
            is SocketRemoteMessage.Plain -> {
                logger.info("Received PLAIN message: ${message.data}")
                if (message.data == "Ping") {
                    send(client, writeChannel, SocketRemoteMessage.Plain("Pong"))
                } else {}
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
            
            is SocketRemoteMessage.SessionAuth,
            is SocketRemoteMessage.SessionAck -> {
                logger.warn("Received unexpected session message after handshake")
            }
        }
    } catch (e: Exception) {
        logger.error("Failed to parse message: ", e)
    }

    private fun disconnectClientById(socketId: String) {
        with(sockets.remove(socketId) ?: return) {
            try {
                SessionCleanupManager.markOffline(socketId)
                // We do NOT remove from WSDataManager immediately so data persists for resume.
                // Cleanup manager will handle real removal.
                close()
                logger.info("Client socket[$socketId] disconnected")
            } catch (e: Exception) {
                logger.error("Cannot disconnect socket[$socketId]", e)
            }
        }
    }

    private class SocketDataException : IllegalArgumentException("Cannot read message, data is null")

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun ByteWriteChannel.writeFramedJsonSafe(data: SocketRemoteMessage, compress: Boolean) {
        val packet = buildPacket {
            if (compress) {
                val gzip = GZIPOutputStream(asOutputStream())
                gzip.use {
                    json.encodeToStream(SocketRemoteMessage.serializer(), data, it)
                    gzip.finish()
                }
            } else {
                asOutputStream().use {
                    json.encodeToStream(SocketRemoteMessage.serializer(), data, it)
                }
            }
        }
        val size = packet.remaining
        require(size >= 0) { "Negative frame size" }
        writeLong(size)
        writePacket(packet)
        flush()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun ByteReadChannel.readFramedJsonSafe(
        decompress: Boolean,
        maxFrameSize: Long = SocketConstants.DEFAULT_MAX_FRAME_SIZE
    ): SocketRemoteMessage = withContext(Dispatchers.IO) {
        val size = readLong()
        if (size !in 1..maxFrameSize) {
            throw IllegalStateException("Invalid frame size: $size")
        }

        val packet = readPacket(size.toInt())

        val stream = if (decompress) {
            GZIPInputStream(packet.inputStream())
        } else {
            packet.inputStream()
        }
        try {
            stream.use {
                json.decodeFromStream(SocketRemoteMessage.serializer(), it)
            }
        } catch (e: SerializationException) {
            throw IllegalStateException("Corrupted JSON frame", e)
        }
    }
}