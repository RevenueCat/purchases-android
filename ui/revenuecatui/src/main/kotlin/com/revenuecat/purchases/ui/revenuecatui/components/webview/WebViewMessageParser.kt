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
 * Validation rules (see the RevenueCat `web_view` postMessage protocol):
 * - The payload must not exceed [MAX_PAYLOAD_BYTES] and must not nest deeper than [MAX_NESTING_DEPTH].
 * - The body must be a JSON object with a non-empty string `type` and a string `component_id`.
 * - `component_id` must equal the expected component id; otherwise the message is rejected.
 * - `rc:step-complete` may carry a `responses` JSON object of JSON-compatible values.
 * - `rc:error` must carry a string `error`.
 * - `rc:step-loaded` and `rc:request-variables` require no extra fields.
 * - Unknown message types are dropped for v1.
 *
 * Returns `null` for any payload that is too large, malformed, fails validation, targets a different
 * component, or has an unknown type.
 */
internal object WebViewMessageParser {

    const val MAX_PAYLOAD_BYTES: Int = 65_536
    const val MAX_NESTING_DEPTH: Int = 16

    @Suppress("ReturnCount")
    fun parse(rawJson: String, expectedComponentId: String): PaywallWebViewMessage? {
        if (rawJson.toByteArray(Charsets.UTF_8).size > MAX_PAYLOAD_BYTES) {
            Logger.w("Dropping web view message: payload exceeds $MAX_PAYLOAD_BYTES bytes.")
            return null
        }

        val json = try {
            JSONObject(rawJson)
        } catch (@Suppress("SwallowedException") e: org.json.JSONException) {
            Logger.w("Dropping web view message: body is not a JSON object.")
            return null
        }

        val type = (json.opt(WebViewMessageField.TYPE) as? String)?.takeIf { it.isNotEmpty() }
        if (type == null) {
            Logger.w("Dropping web view message: missing or non-string 'type'.")
            return null
        }

        val componentId = json.opt(WebViewMessageField.COMPONENT_ID) as? String
        if (componentId == null) {
            Logger.w("Dropping web view message: missing or non-string 'component_id'.")
            return null
        }
        if (componentId != expectedComponentId) {
            Logger.w("Dropping web view message: 'component_id' does not match the rendered web_view.")
            return null
        }

        return when (type) {
            WebViewMessageType.STEP_LOADED,
            WebViewMessageType.REQUEST_VARIABLES,
            -> PaywallWebViewMessage(componentId = componentId, type = type)

            WebViewMessageType.STEP_COMPLETE -> {
                val responses = parseResponses(json) ?: return null
                PaywallWebViewMessage(componentId = componentId, type = type, responses = responses.value)
            }

            WebViewMessageType.ERROR -> {
                val error = json.opt(WebViewMessageField.ERROR) as? String
                if (error == null) {
                    Logger.w("Dropping web view message: 'rc:error' requires a string 'error'.")
                    return null
                }
                PaywallWebViewMessage(componentId = componentId, type = type, error = error)
            }

            else -> {
                // Unknown message types are dropped for protocol_version 1.
                Logger.d("Dropping web view message: unknown type '$type'.")
                null
            }
        }
    }

    /**
     * Parses the optional `responses` object of a `rc:step-complete` message. Returns an empty object
     * when absent, or `null` when present but malformed (not a JSON object, non-JSON values, or too
     * deeply nested), which causes the whole message to be rejected.
     */
    private class ParsedResponses(val value: Map<String, PaywallWebViewValue>)

    @Suppress("ReturnCount")
    private fun parseResponses(json: JSONObject): ParsedResponses? {
        if (!json.has(WebViewMessageField.RESPONSES) || json.isNull(WebViewMessageField.RESPONSES)) {
            return ParsedResponses(emptyMap())
        }
        val responsesJson = json.opt(WebViewMessageField.RESPONSES) as? JSONObject
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
}
