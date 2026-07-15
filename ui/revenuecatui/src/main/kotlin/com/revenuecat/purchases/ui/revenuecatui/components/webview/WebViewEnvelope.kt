@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import org.json.JSONObject

/**
 * Wire-format envelopes for the `workflow-web-components-sdk` transport layer.
 *
 * Matches [RevenueCat/workflow-web-components-sdk](https://github.com/RevenueCat/workflow-web-components-sdk):
 * channel `rc-web-components`, handshake kinds `connect` / `init` / `reject`, and app frames
 * `message` / `request` / `response` / `error`.
 *
 * On Android the host installs `rcWebComponents` via `WebViewCompat.addWebMessageListener` so inbound
 * frames carry a platform `sourceOrigin` / `isMainFrame`. The content still calls
 * `rcWebComponents.postMessage(jsonString)`.
 */
internal object WebViewEnvelope {

    const val CHANNEL: String = "rc-web-components"
    const val NATIVE_OBJECT_NAME: String = "rcWebComponents"
    const val RECEIVE_FUNCTION: String = "__rcWebComponentsReceive"
    const val DEFAULT_PROTOCOL_VERSION: Int = 1

    /** Maximum UTF-8 byte length accepted for a single inbound frame. */
    const val MAX_PAYLOAD_BYTES: Int = 65_536

    /**
     * Maximum nesting depth for a payload tree. The whole-frame pre-parse scan allows one extra
     * level for the envelope object itself ([MAX_FRAME_DEPTH]).
     */
    const val MAX_NESTING_DEPTH: Int = 16

    const val KIND_CONNECT: String = "connect"
    const val KIND_INIT: String = "init"
    const val KIND_REJECT: String = "reject"
    const val KIND_MESSAGE: String = "message"
    const val KIND_REQUEST: String = "request"
    const val KIND_RESPONSE: String = "response"
    const val KIND_ERROR: String = "error"

    private val ENVELOPE_KINDS: Set<String> = setOf(
        KIND_CONNECT,
        KIND_INIT,
        KIND_REJECT,
        KIND_MESSAGE,
        KIND_REQUEST,
        KIND_RESPONSE,
        KIND_ERROR,
    )

    internal data class Parsed(
        val kind: String,
        val protocolVersion: Int,
        val componentId: String,
        val type: String?,
        val id: String?,
        val payload: JSONObject?,
        val error: String?,
    )

    /**
     * Coarse structural depth budget for a whole inbound frame: the envelope object itself plus a
     * payload tree of at most [MAX_NESTING_DEPTH] levels. The precise per-tree limit is still
     * enforced during app-message conversion; this scan exists only to stop hostile input before
     * recursion happens.
     */
    private const val MAX_FRAME_DEPTH: Int = MAX_NESTING_DEPTH + 1

    @Suppress("ReturnCount", "CyclomaticComplexMethod")
    fun parse(rawJson: String): Parsed? {
        if (rawJson.toByteArray(Charsets.UTF_8).size > MAX_PAYLOAD_BYTES) return null

        // Enforce the nesting budget BEFORE org.json parses: JSONTokener recurses per nesting
        // level, and tens of thousands of levels fit inside the 64 KiB frame limit, so a hostile
        // deeply-nested frame could otherwise overflow the stack before any post-parse check runs.
        if (exceedsMaxDepth(rawJson)) return null

        val json = try {
            JSONObject(rawJson)
        } catch (@Suppress("SwallowedException") _: org.json.JSONException) {
            return null
        }

        if (json.opt(WebViewMessageField.CHANNEL) != CHANNEL) return null

        val protocolVersion = json.opt(WebViewMessageField.PROTOCOL_VERSION)
        if (protocolVersion !is Number || !protocolVersion.toDouble().isFinite()) return null

        val kind = json.opt(WebViewMessageField.KIND) as? String
        if (kind == null || kind !in ENVELOPE_KINDS) return null

        val componentId = json.opt(WebViewMessageField.COMPONENT_ID) as? String ?: return null

        val type = json.opt(WebViewMessageField.TYPE) as? String
        if (json.has(WebViewMessageField.TYPE) && type == null) return null

        val id = json.opt(WebViewMessageField.ID) as? String
        if (json.has(WebViewMessageField.ID) && id == null) return null

        val error = json.opt(WebViewMessageField.ERROR) as? String
        if (json.has(WebViewMessageField.ERROR) && error == null) return null

        val payload = when (val payloadValue = json.opt(WebViewMessageField.PAYLOAD)) {
            null, JSONObject.NULL -> null
            is JSONObject -> payloadValue
            else -> return null
        }

        return Parsed(
            kind = kind,
            protocolVersion = protocolVersion.toInt(),
            componentId = componentId,
            type = type,
            id = id,
            payload = payload,
            error = error,
        )
    }

    /**
     * Non-recursive scan of the raw JSON characters, tracking `{`/`[` nesting outside string
     * literals (honoring `\` escapes). Returns `true` when the depth exceeds [MAX_FRAME_DEPTH].
     */
    @Suppress("LoopWithTooManyJumpStatements")
    fun exceedsMaxDepth(rawJson: String): Boolean {
        var depth = 0
        var inString = false
        var escaped = false

        for (char in rawJson) {
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                continue
            }

            when (char) {
                '"' -> inString = true
                '{', '[' -> {
                    depth += 1
                    if (depth > MAX_FRAME_DEPTH) return true
                }
                '}', ']' -> depth -= 1
                else -> Unit
            }
        }

        return false
    }

    @Suppress("LongParameterList")
    fun build(
        kind: String,
        protocolVersion: Int,
        componentId: String,
        type: String? = null,
        id: String? = null,
        payload: JSONObject? = null,
        error: String? = null,
    ): JSONObject = JSONObject().apply {
        put(WebViewMessageField.CHANNEL, CHANNEL)
        put(WebViewMessageField.PROTOCOL_VERSION, protocolVersion)
        put(WebViewMessageField.KIND, kind)
        put(WebViewMessageField.COMPONENT_ID, componentId)
        type?.let { put(WebViewMessageField.TYPE, it) }
        id?.let { put(WebViewMessageField.ID, it) }
        payload?.let { put(WebViewMessageField.PAYLOAD, it) }
        error?.let { put(WebViewMessageField.ERROR, it) }
    }
}
