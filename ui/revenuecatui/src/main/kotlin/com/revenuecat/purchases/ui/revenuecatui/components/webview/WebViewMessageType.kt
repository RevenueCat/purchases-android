@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

/**
 * Message type identifiers for the RevenueCat `web_view` postMessage protocol (`protocol_version: 1`).
 * These mirror the shapes used by the web implementation and must not diverge.
 */
internal object WebViewMessageType {
    /** Host → content: which axes the native host sizes to the content (`fit`). */
    const val FIT = "fit"

    /** Content → host: reported content box size in CSS pixels. */
    const val RESIZE = "resize"
}
