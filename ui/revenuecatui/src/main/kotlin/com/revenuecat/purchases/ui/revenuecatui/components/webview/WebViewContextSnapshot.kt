@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@JvmSynthetic
internal fun webViewContextSnapshot(
    state: PaywallState.Loaded.Components,
    darkMode: Boolean,
): JsonObject = webViewContextSnapshot(
    customVariables = state.mergedCustomVariables,
    locale = state.locale.toLanguageTag(),
    darkMode = darkMode,
)

/**
 * Builds the payload the handshake seeds and later `context` pushes replace, per the
 * custom component variables contract (RevenueCat/docs#1874): absent structured sections are
 * literal `null`, empty maps are `{}`, and `workflow` is omitted entirely outside a funnel.
 *
 * @param locale a BCP-47 language tag. The content SDK feeds it to `Intl`, which rejects the
 * underscored `LocaleId` form with a `RangeError`.
 */
@JvmSynthetic
internal fun webViewContextSnapshot(
    customVariables: Map<String, CustomVariableValue>,
    locale: String,
    darkMode: Boolean,
): JsonObject = buildJsonObject {
    putJsonObject(Keys.CUSTOM) {
        customVariables.forEach { (name, value) -> put(name, value.asJsonPrimitive) }
    }
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

private val CustomVariableValue.asJsonPrimitive: JsonPrimitive
    get() = map(
        string = { JsonPrimitive(it) },
        number = { number ->
            // A bare NaN or Infinity is not valid JSON.
            if (!number.isFinite()) {
                JsonPrimitive(number.toString())
            } else {
                val whole = number.toLong()
                if (number == whole.toDouble()) JsonPrimitive(whole) else JsonPrimitive(number)
            }
        },
        boolean = { JsonPrimitive(it) },
    )
