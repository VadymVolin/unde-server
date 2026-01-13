package com.unde.server.socket.remote

import com.unde.server.constants.JsonToken
import com.unde.server.socket.WSDataManager
import com.unde.server.socket.remote.model.SocketRemoteMessage
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.logging.*
import io.ktor.utils.io.*
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

internal object ServerSocketConnection {
    private val logger = KtorSimpleLogger(javaClass.simpleName)

    private val json = Json {
        classDiscriminator = JsonToken.TYPE_TOKEN
        ignoreUnknownKeys = true
    }

    private val selectorManager = SelectorManager(Dispatchers.IO)
    private var socketScope: CoroutineScope? = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private val sockets = mutableMapOf<String, Socket>()

    internal fun connect(host: String, port: Int) {
        val scope = socketScope ?: CoroutineScope(Dispatchers.IO).also { socketScope = it }
        scope.launch { // server coroutine
            logger.info("Connecting server socket to $host:$port")
            serverSocket = aSocket(selectorManager).tcp().bind(host, port).also { ss ->
                while (isActive) {
                    logger.info("Checking new connection:")
                    var socketId = ""
                    try {
                        with(ss.accept()) {
                            socketId = remoteAddress.toString()
                            connectClientById(socketId, this)
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

    internal fun disconnect() {
        // todo: release sockets map here
        serverSocket?.close()
        socketScope?.cancel()
    }

    private fun connectClientById(clientId: String, client: Socket) {
        sockets[clientId] = client
        logger.info("Accepted connection to client socket: $clientId")
        client.launch {
            // todo: extract it to a separate method and add try catch handling
            val readChannel = client.openReadChannel()
            val writeChannel = client.openWriteChannel(true)
            while (!client.isClosed && !readChannel.isClosedForRead) {
                try {
                    val byteArray = ByteArray(readChannel.readUTF8Line()?.toInt() ?: error("Frame size wasn't specified"))
                    ensureActive()
                    readChannel.readFully(byteArray)
                    ensureActive()
                    val data = byteArray.decodeToString()
                    handleReceivedData(clientId, data)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logger.warn("Failed to read data from client $clientId, data is null, start disconnection")
                    // todo: check how we disconnect from the client, exception is not the best scenario for this case, maybe custom one, or simply track null line
                    disconnectClientById(clientId)
                    break
                }
            }
        }
    }

//    private fun send(socket, message)

    private fun handleReceivedData(clientId: String, data: String) {
        try {
            logger.warn("Received data from client $clientId: $data")
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
            }
        } catch (e: Exception) {
            logger.error("Failed to parse message: ${e.message}")
        }
    }

    private fun disconnectClientById(socketId: String) {
        with(sockets.remove(socketId) ?: return) {
            cancel()
            dispose()
            logger.info("Client socket disconnected")
        }
    }
}