package com.unde.server.socket.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an intercepted network request.
 *
 * @property requestTime Timestamp of the request.
 * @property url The request URL.
 * @property method HTTP method (GET, POST, etc.).
 * @property headers Request headers.
 * @property body Request body content.
 */
@Serializable
internal data class UndeRequest(
    @SerialName("requestTime")
    val requestTime: Long,
    @SerialName("url")
    val url: String,
    @SerialName("method")
    val method: String,
    @SerialName("headers")
    val headers: Map<String, List<String>>,
    @SerialName("body")
    val body: String? = null
)

/**
 * Represents an intercepted network response.
 *
 * @property responseTime Timestamp of the response.
 * @property code HTTP status code.
 * @property message HTTP status message.
 * @property headers Response headers.
 * @property protocol Protocol used (e.g., http/1.1).
 * @property body Response body content.
 */
@Serializable
internal data class UndeResponse(
    @SerialName("responseTime")
    val responseTime: Long,
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

/**
 * Wraps a paired request and response.
 */
@Serializable
internal data class UndeRequestResponse(
    @SerialName("request")
    val request: UndeRequest,
    @SerialName("response")
    val response: UndeResponse
)