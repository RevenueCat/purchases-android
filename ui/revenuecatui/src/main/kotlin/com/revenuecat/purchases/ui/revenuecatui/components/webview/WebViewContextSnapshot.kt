@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Builds the payload shared by the `context` push and the `requestContext` response, per the
 * custom component variables contract (RevenueCat/docs#1874): absent structured sections are
 * literal `null`, empty maps are `{}`, and `workflow` is omitted entirely outside a funnel.
 */
@JvmSynthetic
internal fun webViewContextSnapshot(
    locale: String,
    darkMode: Boolean,
): JsonObject = buildJsonObject {
    putJsonObject("custom") {}
    put("offering", JsonNull)
    putJsonArray("packages") {}
    put("package", JsonNull)
    put("selected_package", JsonNull)
    putJsonObject("inputs") {}
    putJsonObject("device_meta") {
        put("is_preview", false)
        put("locale", locale)
        put("dark_mode", darkMode)
        put("updated_at", System.currentTimeMillis())
    }
}
