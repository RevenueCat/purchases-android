@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewMessage
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewValue
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import org.json.JSONObject

/**
 * Parses and validates raw JSON strings received from a `web_view` component into typed
 * [PaywallWebViewMessage]s before they reach app code.
 *
 * Payloads use the `workflow-web-components-sdk` transport envelope (`channel`,
 * `protocol_version`, `kind`, `component_id`, …). App-level messages arrive as `kind`
 * `message` or `request` with a `type` such as `rc:step-loaded`.
 *
 * Returns `null` for handshake frames, transport errors, malformed payloads, wrong
 * `component_id`, or unknown app message types.
 */
internal object WebViewMessageParser {

    const val MAX_PAYLOAD_BYTES: Int = 65_536
    const val MAX_NESTING_DEPTH: Int = 16

    internal data class ParsedAppMessage(
        val message: PaywallWebViewMessage,
        /** Set when the inbound frame is a transport `request` that expects a `response`. */
        val requestId: String? = null,
        val requestType: String? = null,
    )

    @Suppress("ReturnCount")
    fun parse(rawJson: String, expectedComponentId: String): ParsedAppMessage? {
        if (rawJson.toByteArray(Charsets.UTF_8).size > MAX_PAYLOAD_BYTES) {
            Logger.w("Dropping web view message: payload exceeds $MAX_PAYLOAD_BYTES bytes.")
            return null
        }

        val envelope = WebViewEnvelope.parse(rawJson) ?: run {
            Logger.w("Dropping web view message: not a valid transport envelope.")
            return null
        }

        return when (envelope.kind) {
            WebViewEnvelope.KIND_MESSAGE,
            WebViewEnvelope.KIND_REQUEST,
            -> parseAppMessage(envelope, expectedComponentId)

            else -> null
        }
    }

    @Suppress("ReturnCount")
    private fun parseAppMessage(
        envelope: WebViewEnvelope.Parsed,
        expectedComponentId: String,
    ): ParsedAppMessage? {
        if (envelope.componentId != expectedComponentId) {
            Logger.w("Dropping web view message: 'component_id' does not match the rendered web_view.")
            return null
        }

        val type = envelope.type?.takeIf { it.isNotEmpty() }
        if (type == null) {
            Logger.w("Dropping web view message: missing or non-string 'type'.")
            return null
        }

        val message = when (type) {
            WebViewMessageType.STEP_LOADED,
            WebViewMessageType.REQUEST_VARIABLES,
            -> PaywallWebViewMessage(componentId = envelope.componentId, type = type)

            WebViewMessageType.STEP_COMPLETE -> {
                val responses = parseResponses(envelope.payload) ?: return null
                PaywallWebViewMessage(
                    componentId = envelope.componentId,
                    type = type,
                    responses = responses.value,
                )
            }

            WebViewMessageType.ERROR -> {
                val error = parseError(envelope) ?: return null
                PaywallWebViewMessage(componentId = envelope.componentId, type = type, error = error)
            }

            else -> {
                Logger.d("Dropping web view message: unknown type '$type'.")
                return null
            }
        }

        val requestId = if (envelope.kind == WebViewEnvelope.KIND_REQUEST) envelope.id else null
        if (envelope.kind == WebViewEnvelope.KIND_REQUEST && requestId == null) {
            Logger.w("Dropping web view message: transport request is missing 'id'.")
            return null
        }

        return ParsedAppMessage(
            message = message,
            requestId = requestId,
            requestType = type,
        )
    }

    private fun parseError(envelope: WebViewEnvelope.Parsed): String? {
        val payloadError = envelope.payload?.opt(WebViewMessageField.ERROR) as? String
        val error = payloadError ?: envelope.error
        if (error == null) {
            Logger.w("Dropping web view message: 'rc:error' requires a string 'error'.")
            return null
        }
        return error
    }

    /**
     * Parses the `responses` object for a `rc:step-complete` message. The responses may live under
     * `payload.responses` or directly in `payload` when the content sends only response fields.
     */
    private class ParsedResponses(val value: Map<String, PaywallWebViewValue>)

    @Suppress("ReturnCount")
    private fun parseResponses(payload: JSONObject?): ParsedResponses? {
        if (payload == null || payload.length() == 0) {
            return ParsedResponses(emptyMap())
        }

        val responsesJson = when {
            payload.has(WebViewMessageField.RESPONSES) && !payload.isNull(WebViewMessageField.RESPONSES) ->
                payload.opt(WebViewMessageField.RESPONSES) as? JSONObject

            payload.keys().asSequence().any { it in STEP_COMPLETE_RESERVED_PAYLOAD_KEYS } -> {
                Logger.w(
                    "Dropping web view message: 'rc:step-complete' payload contains transport fields " +
                        "without a 'responses' object.",
                )
                return null
            }

            else -> payload
        }

        if (responsesJson == null) {
            Logger.w("Dropping web view message: 'responses' must be a JSON object.")
            return null
        }

        val converted = PaywallWebViewValue.fromJson(responsesJson, MAX_NESTING_DEPTH)
        if (converted !is PaywallWebViewValue.Object) {
            Logger.w("Dropping web view message: 'responses' contains non-JSON values or is too deeply nested.")
            return null
        }
        return ParsedResponses(converted.value)
    }

    private val STEP_COMPLETE_RESERVED_PAYLOAD_KEYS: Set<String> = setOf(
        WebViewMessageField.CHANNEL,
        WebViewMessageField.PROTOCOL_VERSION,
        WebViewMessageField.KIND,
        WebViewMessageField.TYPE,
        WebViewMessageField.COMPONENT_ID,
        WebViewMessageField.ID,
        WebViewMessageField.ERROR,
        WebViewMessageField.VARIABLES,
    )
}
