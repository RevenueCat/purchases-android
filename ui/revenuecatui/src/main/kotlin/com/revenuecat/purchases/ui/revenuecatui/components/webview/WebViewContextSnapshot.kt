@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Builds the payload the handshake seeds and later `context` pushes replace, per the
 * custom component variables contract (RevenueCat/docs#1874): absent structured sections are
 * literal `null`, empty maps are `{}`, and `workflow` is omitted entirely outside a funnel.
 */
@JvmSynthetic
internal fun webViewContextSnapshot(
    locale: String,
    darkMode: Boolean,
): JsonObject = buildJsonObject {
    putJsonObject(Keys.CUSTOM) {}
    put(Keys.OFFERING, JsonNull)
    putJsonArray(Keys.PACKAGES) {}
    put(Keys.PACKAGE, JsonNull)
    put(Keys.SELECTED_PACKAGE, JsonNull)
    putJsonObject(Keys.INPUTS) {}
    putJsonObject(Keys.DEVICE_META) {
        put(Keys.IS_PREVIEW, false)
        put(Keys.LOCALE, locale)
        put(Keys.DARK_MODE, darkMode)
        put(Keys.UPDATED_AT, System.currentTimeMillis())
    }
}

/** Wire keys from the contract (RevenueCat/docs#1874), in the order its table lists them. */
private object Keys {
    const val CUSTOM = "custom"
    const val OFFERING = "offering"
    const val PACKAGES = "packages"
    const val PACKAGE = "package"
    const val SELECTED_PACKAGE = "selected_package"
    const val INPUTS = "inputs"
    const val DEVICE_META = "device_meta"

    const val IS_PREVIEW = "is_preview"
    const val LOCALE = "locale"
    const val DARK_MODE = "dark_mode"
    const val UPDATED_AT = "updated_at"
}
