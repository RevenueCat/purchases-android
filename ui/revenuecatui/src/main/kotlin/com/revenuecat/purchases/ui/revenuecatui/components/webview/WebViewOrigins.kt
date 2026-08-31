@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.net.Uri
import java.util.Locale

/**
 * The origin of this URL as `scheme://host[:port]`, or `null` if it lacks a host. Host comparison is
 * case-insensitive. Default ports (443 for https, 80 for http) are omitted.
 *
 * Accepts both full URLs and bare origins (as provided by [WebViewCompat.WebMessageListener]).
 */
@Suppress("ReturnCount", "MagicNumber")
internal fun String.toOriginOrNull(): String? {
    val uri = Uri.parse(this)
    return uri.toOriginOrNull()
}

/**
 * The origin of this [Uri] as `scheme://host[:port]`, or `null` if it lacks a host.
 */
@Suppress("ReturnCount", "MagicNumber")
internal fun Uri.toOriginOrNull(): String? {
    val scheme = scheme?.lowercase(Locale.US) ?: return null
    val host = host?.takeIf { it.isNotBlank() }?.lowercase(Locale.US) ?: return null
    val portSuffix = when {
        port == -1 -> ""
        scheme == "https" && port == 443 -> ""
        scheme == "http" && port == 80 -> ""
        else -> ":$port"
    }
    return "$scheme://$host$portSuffix"
}

/**
 * Navigation policy for `web_view` content, applied from `shouldOverrideUrlLoading`:
 *
 * - Any non-HTTPS navigation is blocked (any frame).
 * - Main-frame navigation is additionally restricted to the resolved component URL's origin
 *   (same-origin different-path navigation stays allowed). This makes cross-origin message races
 *   structurally impossible; the bridge's per-message origin check remains as defense in depth.
 * - Cross-origin sub-frame loads are not blocked here; isolation for those is expected from the
 *   server-provided Content-Security-Policy served with the web content.
 */
@Suppress("ReturnCount")
internal fun shouldBlockWebViewNavigation(
    url: String?,
    isMainFrame: Boolean,
    expectedOrigin: String?,
): Boolean {
    val origin = url?.toOriginOrNull() ?: return true
    if (!origin.startsWith("https://")) return true
    val normalizedExpectedOrigin = expectedOrigin?.toOriginOrNull()
    return isMainFrame && origin != normalizedExpectedOrigin
}
