@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewValue
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * The native color scheme reported to web content as the `color_scheme` SDK-managed variable.
 */
internal enum class WebViewColorScheme(val jsonValue: String) {
    LIGHT("light"),
    DARK("dark"),
    UNKNOWN("unknown"),
}

/**
 * Builds the `variables` payload sent to a `web_view` component in response to `rc:request-variables`.
 *
 * Produces a flat map shaped like:
 * ```json
 * { "locale": "en-US", "color_scheme": "dark", "custom": { ... } }
 * ```
 *
 * Only safe, already-available context is exposed: the paywall locale, the color scheme, and the
 * paywall's custom variables. API keys, email addresses, the Purchases SDK instance, and storage are
 * never included.
 *
 * [KEY_LOCALE] and [KEY_COLOR_SCHEME] are reserved SDK-managed top-level keys: app-provided variables
 * may not overwrite them and should be nested under [KEY_CUSTOM].
 */
internal object PaywallWebViewVariablesProvider {

    const val KEY_LOCALE: String = "locale"
    const val KEY_COLOR_SCHEME: String = "color_scheme"
    const val KEY_CUSTOM: String = "custom"

    val reservedKeys: Set<String> = setOf(KEY_LOCALE, KEY_COLOR_SCHEME)

    /**
     * The SDK-managed variables plus the paywall's custom variables (nested under [KEY_CUSTOM]).
     *
     * @param locale a BCP-47-ish locale string, e.g. `en-US`.
     * @param colorScheme the current color scheme.
     * @param customVariables the paywall's custom variables (dashboard defaults merged with runtime overrides).
     */
    fun sdkManagedVariables(
        locale: String,
        colorScheme: WebViewColorScheme,
        customVariables: Map<String, CustomVariableValue>,
    ): Map<String, PaywallWebViewValue> {
        val custom = customVariables.mapValues { (_, value) -> value.toPaywallWebViewValue() }
        return linkedMapOf(
            KEY_LOCALE to PaywallWebViewValue.String(locale),
            KEY_COLOR_SCHEME to PaywallWebViewValue.String(colorScheme.jsonValue),
            KEY_CUSTOM to PaywallWebViewValue.Object(custom),
        )
    }

    /**
     * Removes reserved SDK-managed top-level keys from app-provided variables so they cannot overwrite
     * SDK values, logging a warning for any that were dropped.
     */
    fun sanitizeAppProvidedVariables(
        variables: Map<String, PaywallWebViewValue>,
    ): Map<String, PaywallWebViewValue> {
        val sanitized = variables.filterKeys { key ->
            val reserved = key in reservedKeys
            if (reserved) {
                Logger.w(
                    "Ignoring reserved web view variable key '$key'. Reserved SDK-managed keys cannot be " +
                        "overwritten; nest app values under '$KEY_CUSTOM' instead.",
                )
            }
            !reserved
        }
        return sanitized
    }
}

internal fun CustomVariableValue.toPaywallWebViewValue(): PaywallWebViewValue = when (this) {
    is CustomVariableValue.String -> PaywallWebViewValue.String(value)
    is CustomVariableValue.Number -> PaywallWebViewValue.Number(value)
    is CustomVariableValue.Boolean -> PaywallWebViewValue.Boolean(value)
    else -> PaywallWebViewValue.Null
}
