@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Wire-format envelope for the `workflow-web-components-sdk` transport layer.
 *
 * Matches [RevenueCat/workflow-web-components-sdk](https://github.com/RevenueCat/workflow-web-components-sdk):
 * channel `rc-web-components`, handshake kinds `connect` / `init` / `reject`, and app frames
 * `message` / `request` / `response` / `error`.
 *
 * On Android the host installs `rcWebComponents` via `WebViewCompat.addWebMessageListener` so inbound
 * frames carry a platform `sourceOrigin` / `isMainFrame`. The content still calls
 * `rcWebComponents.postMessage(jsonString)`.
 */
@Serializable
internal data class WebViewEnvelope(
    // @Required: a frame missing `channel` must fail to decode despite the construction-side default.
    @Required val channel: String = CHANNEL,
    @SerialName("protocol_version") val protocolVersion: Int,
    val kind: Kind,
    @SerialName("component_id") val componentId: String,
    val type: String? = null,
    val id: String? = null,
    val payload: JsonObject? = null,
    val error: String? = null,
) {

    @Serializable
    enum class Kind {
        @SerialName("connect")
        CONNECT,

        @SerialName("init")
        INIT,

        @SerialName("reject")
        REJECT,

        @SerialName("message")
        MESSAGE,

        @SerialName("request")
        REQUEST,

        @SerialName("response")
        RESPONSE,

        @SerialName("error")
        ERROR,
    }

    fun toJsonString(): String = json.encodeToString(serializer(), this)

    companion object {
        const val CHANNEL: String = "rc-web-components"
        const val NATIVE_OBJECT_NAME: String = "rcWebComponents"
        const val RECEIVE_FUNCTION: String = "__rcWebComponentsReceive"
        const val DEFAULT_PROTOCOL_VERSION: Int = 1

        private val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        /**
         * Parses an inbound frame, or `null` when it is not a valid `rc-web-components` envelope:
         * malformed JSON, wrong or missing `channel`, unknown `kind`, or any field whose JSON type
         * does not match the schema.
         */
        fun parse(rawJson: String): WebViewEnvelope? = try {
            json.decodeFromString(serializer(), rawJson).takeIf { it.channel == CHANNEL }
        } catch (@Suppress("SwallowedException") _: SerializationException) {
            null
        }
    }
}
