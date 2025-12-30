package com.unde.server.socket.client.model

import com.unde.server.constants.JsonToken
import com.unde.server.socket.model.UndeRequestResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal sealed interface WSLocalMessage {
    @Serializable
    @SerialName(JsonToken.TYPE_CONNECTIONS_LIST_TOKEN)
    data class ConnectionsList(val connections: List<String>): WSLocalMessage
    
    @Serializable
    @SerialName(JsonToken.TYPE_NETWORK_TOKEN)
    data class Network(val connectionId: String, val data: UndeRequestResponse): WSLocalMessage
    
    @Serializable
    @SerialName(JsonToken.TYPE_DATABASE_TOKEN)
    data class Database(val connectionId: String, val data: JsonObject): WSLocalMessage
    
    @Serializable
    @SerialName(JsonToken.TYPE_LOGCAT_TOKEN)
    data class Logcat(val connectionId: String, val data: JsonObject): WSLocalMessage
    
    @Serializable
    @SerialName(JsonToken.TYPE_SELECT_CONNECTION_TOKEN)
    data class SelectConnection(val connectionId: String): WSLocalMessage
}
