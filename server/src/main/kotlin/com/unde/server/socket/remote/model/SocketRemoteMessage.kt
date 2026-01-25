package com.unde.server.socket.remote.model

import com.unde.server.constants.JsonToken
import com.unde.server.socket.model.UndeRequestResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement


@Serializable
internal sealed interface SocketRemoteMessage {

    @Serializable
    @SerialName(JsonToken.TYPE_RESULT_TOKEN)
    data class Result(val data: String) : SocketRemoteMessage

    @Serializable
    @SerialName(JsonToken.TYPE_COMMAND_TOKEN)
    data class Command(val data: JsonObject) : SocketRemoteMessage

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
}


internal fun createResultCommandData(data: String): JsonObject = JsonObject(
    mapOf(JsonToken.RESULT_TOKEN to Json.encodeToJsonElement(data))
)
