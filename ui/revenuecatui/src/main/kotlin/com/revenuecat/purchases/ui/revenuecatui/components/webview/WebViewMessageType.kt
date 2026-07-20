@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

/**
 * Message type identifiers for the RevenueCat `web_view` postMessage protocol (`protocol_version: 1`).
 * These mirror the shapes used by the web implementation and must not diverge.
 */
internal object WebViewMessageType {
    const val STEP_LOADED = "rc:step-loaded"
    const val STEP_COMPLETE = "rc:step-complete"
    const val REQUEST_VARIABLES = "rc:request-variables"
    const val ERROR = "rc:error"
    const val VARIABLES = "rc:variables"

    /** Host → content: which axes the native host sizes to the content (`fit`). */
    const val FIT = "fit"

    /** Content → host: reported content box size in CSS pixels. */
    const val RESIZE = "resize"
}

/**
 * Field names used in the transport envelope parsed by [WebViewEnvelope]. Application data lives under
 * [PAYLOAD]; [ERROR] is used on `error` / `reject` frames.
 */
internal object WebViewMessageField {
    const val CHANNEL = "channel"
    const val PROTOCOL_VERSION = "protocol_version"
    const val KIND = "kind"
    const val TYPE = "type"
    const val COMPONENT_ID = "component_id"
    const val ID = "id"
    const val PAYLOAD = "payload"
    const val ERROR = "error"
}
