package com.unde.server.socket.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UndeRequest(
    @SerialName("url")
    val url: String,
    @SerialName("method")
    val method: String,
    @SerialName("headers")
    val headers: Map<String, List<String>>,
    @SerialName("body")
    val body: String? = null
)

@Serializable
internal data class UndeResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("headers")
    val headers: Map<String, List<String>>,
    @SerialName("protocol")
    val protocol: String,
    @SerialName("body")
    val body: String?
)

@Serializable
internal data class UndeRequestResponse(
    @SerialName("request")
    val request: UndeRequest,
    @SerialName("response")
    val response: UndeResponse
)
