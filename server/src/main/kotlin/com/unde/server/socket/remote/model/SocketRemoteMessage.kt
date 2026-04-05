package com.unde.server.socket.remote.model

import com.unde.server.constants.JsonToken
import com.unde.server.socket.model.UndeRequestResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement


/**
 * Messages exchanged with remote Android devices via TCP sockets.
 */
@Serializable
internal sealed interface   SocketRemoteMessage {
    @Serializable
    @SerialName(JsonToken.TYPE_PLAIN_TOKEN)
    data class Plain(val data: String) : SocketRemoteMessage

    @Serializable
    @SerialName(JsonToken.TYPE_NETWORK_TOKEN)
    data class Network(val data: UndeRequestResponse) : SocketRemoteMessage

    @Serializable
    @SerialName(JsonToken.TYPE_DATABASE_TOKEN)
    data class Database(val data: JsonObject) : SocketRemoteMessage

    @Serializable
    @SerialName(JsonToken.TYPE_TELEMETRY_TOKEN)
    data class Telemetry(val data: JsonObject) : SocketRemoteMessage

    @Serializable
    @SerialName(JsonToken.TYPE_LOGCAT_TOKEN)
    data class Logcat(val data: JsonObject) : SocketRemoteMessage

    @Serializable
    @SerialName(JsonToken.TYPE_SESSION_AUTH_TOKEN)
    data class SessionAuth(val sessionId: String) : SocketRemoteMessage

    @Serializable
    @SerialName(JsonToken.TYPE_SESSION_ACK_TOKEN)
    data class SessionAck(val resumed: Boolean) : SocketRemoteMessage
}
