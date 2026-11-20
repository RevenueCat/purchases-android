@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.net.Uri
import com.revenuecat.purchases.ui.revenuecatui.utils.URLOpeningMethod
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale

/** A validated [WebViewMessageType.OPEN_URL] request payload (`{ url, method? }`) from the `web_view` content. */
internal data class WebViewOpenUrlRequest(
    val url: String,
    val method: URLOpeningMethod,
) {

    companion object {
        const val MAX_URL_LENGTH: Int = 2048
        const val MIN_INTERVAL_MILLIS: Long = 1_000L

        // `deep_link` takes app schemes, but never ones that run code, read local data, or build
        // intents. `http` is refused everywhere: app links are `https`.
        private val BLOCKED_DEEP_LINK_SCHEMES = setOf(
            "http",
            "javascript",
            "vbscript",
            "data",
            "file",
            "blob",
            "about",
            "content",
            "intent",
        )

        /**
         * Returns `null` when the request must be refused: `external_browser` and `in_app_browser` take
         * `https` only, `deep_link` any scheme except the blocked ones, and URLs are capped at
         * [MAX_URL_LENGTH] characters with no whitespace or control characters. `method` defaults to
         * `external_browser`.
         */
        @Suppress("ReturnCount")
        fun parse(payload: JsonObject?): WebViewOpenUrlRequest? {
            val url = payload?.stringOrNull("url") ?: return null
            if (url.isEmpty() || url.length > MAX_URL_LENGTH || url.any { it.code <= ' '.code }) return null

            val method = if (payload.containsKey("method")) {
                payload.stringOrNull("method")?.toOpeningMethod() ?: return null
            } else {
                URLOpeningMethod.EXTERNAL_BROWSER
            }

            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase(Locale.US) ?: return null
            val allowed = when (method) {
                URLOpeningMethod.EXTERNAL_BROWSER,
                URLOpeningMethod.IN_APP_BROWSER,
                -> scheme == "https"
                URLOpeningMethod.DEEP_LINK -> scheme !in BLOCKED_DEEP_LINK_SCHEMES
            }
            if (!allowed) return null
            // Embedded credentials can disguise the real host (`https://trusted.com@evil.com`).
            if (scheme == "https" && (uri.host.isNullOrEmpty() || uri.userInfo != null)) return null

            return WebViewOpenUrlRequest(url = url, method = method)
        }

        private fun JsonObject.stringOrNull(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

        private fun String.toOpeningMethod(): URLOpeningMethod? = when (this) {
            "external_browser" -> URLOpeningMethod.EXTERNAL_BROWSER
            "in_app_browser" -> URLOpeningMethod.IN_APP_BROWSER
            "deep_link" -> URLOpeningMethod.DEEP_LINK
            else -> null
        }
    }
}
