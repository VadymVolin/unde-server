package com.unde.server.socket.model

import com.unde.server.constants.JsonToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal sealed interface WSMessage {
    @Serializable
    @SerialName(JsonToken.TYPE_NETWORK_TOKEN)
    data class Network(val data: JsonObject) : WSMessage

    @Serializable
    @SerialName(JsonToken.TYPE_DATABASE_TOKEN)
    data class Database(val data: JsonObject) : WSMessage

    @Serializable
    @SerialName(JsonToken.TYPE_TELEMETRY_TOKEN)
    data class Telemetry(val data: JsonObject) : WSMessage

    @Serializable
    @SerialName(JsonToken.TYPE_LOGCAT_TOKEN)
    data class Logcat(val data: JsonObject) : WSMessage
}