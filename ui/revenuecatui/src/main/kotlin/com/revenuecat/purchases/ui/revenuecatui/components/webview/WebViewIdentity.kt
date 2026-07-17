@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

/**
 * Immutable configuration that identifies a rendered `web_view` instance. When any field changes the
 * Compose tree must dispose the previous [android.webkit.WebView] / bridge and create a new pair —
 * locale and message handler are deliberately excluded so they can update in place.
 */
internal data class WebViewIdentity(
    val resolvedUrl: String,
    val componentId: String?,
    val sizeToContentWidth: Boolean,
    val sizeToContentHeight: Boolean,
)
