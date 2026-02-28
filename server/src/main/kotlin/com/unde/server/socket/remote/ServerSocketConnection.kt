package com.unde.server.socket.remote

import com.unde.server.constants.JsonToken
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
import kotlinx.io.EOFException
import kotlinx.io.asOutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
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

    private fun listenSocketById(clientId: String, client: Socket) =
        client.launch {
            val readChannel = client.openReadChannel()
            val writeChannel = client.openWriteChannel(true)
            while (!client.isClosed && !readChannel.isClosedForRead) {
                logger.info("Listen for a new message: ")
                try {
                    val data = readChannel.readFramedJsonSafe(true)
                    handleReceivedData(client, writeChannel, clientId, data)
                } catch (e: Exception) {
                    when (e) {
                        is CancellationException -> throw e
                        is SocketDataException, is EOFException, is ClosedByteChannelException -> {
                            logger.error("Failed to read data from client $clientId, data is null or channel closed by client, start disconnection", e)
                            disconnectClientById(clientId)
                        }
                        else -> {
                            logger.error("Failed to read data from client $clientId", e)
                        }
                    }
                }
            }
        }

    private fun send(client: Socket, writeChannel: ByteWriteChannel, message: SocketRemoteMessage) =
        client.launch {
            try {
                val encodedJsonString = json.encodeToString(message)
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
        }
    } catch (e: Exception) {
        logger.error("Failed to parse message: ", e)
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

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun ByteWriteChannel.writeFramedJsonSafe(data: SocketRemoteMessage, compress: Boolean) {
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
    suspend fun ByteReadChannel.readFramedJsonSafe(
        decompress: Boolean,
        maxFrameSize: Long = 50L * 1024 * 1024 // 50MB safety limit
    ): SocketRemoteMessage {
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
        return try {
            stream.use {
                json.decodeFromStream(SocketRemoteMessage.serializer(), it)
            }
        } catch (e: SerializationException) {
            throw IllegalStateException("Corrupted JSON frame", e)
        }
    }
}