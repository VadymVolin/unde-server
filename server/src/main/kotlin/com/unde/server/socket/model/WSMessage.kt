package com.unde.server.socket.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class WSMessage {
    @Serializable
    @SerialName("command")
    data class Command(val name: String, val args: Map<String, String>) : WSMessage()

    @Serializable
    @SerialName("response")
    data class Response(val status: String, val data: String) : WSMessage()
}