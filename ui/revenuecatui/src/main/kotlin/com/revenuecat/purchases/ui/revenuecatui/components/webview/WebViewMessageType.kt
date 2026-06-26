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
}

/**
 * Field names used in the flat message envelope. The envelope is intentionally flat: message-specific
 * fields such as [RESPONSES], [ERROR] and [VARIABLES] live at the top level rather than under a
 * generic `payload` key.
 */
internal object WebViewMessageField {
    const val TYPE = "type"
    const val COMPONENT_ID = "component_id"
    const val RESPONSES = "responses"
    const val ERROR = "error"
    const val VARIABLES = "variables"
}
