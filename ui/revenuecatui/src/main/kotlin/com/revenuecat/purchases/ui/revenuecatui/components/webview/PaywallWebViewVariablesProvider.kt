@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewValue
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * Builds the `variables` payload sent to a `web_view` component in response to `rc:request-variables`.
 *
 * Produces a flat map shaped like:
 * ```json
 * { "locale": "en-US" }
 * ```
 *
 * Only safe, already-available system context is exposed: the paywall locale. The paywall's
 * dashboard-defined custom variables are intentionally NOT passed across the bridge in v1. API keys,
 * email addresses, the Purchases SDK instance, and storage are never included.
 *
 * [KEY_LOCALE] is a reserved SDK-managed top-level key: app-provided variables may not overwrite it.
 */
internal object PaywallWebViewVariablesProvider {

    const val KEY_LOCALE: String = "locale"

    val reservedKeys: Set<String> = setOf(KEY_LOCALE)

    /**
     * The SDK-managed system variables exposed to the web view.
     *
     * @param locale a BCP-47-ish locale string, e.g. `en-US`.
     */
    fun sdkManagedVariables(
        locale: String,
    ): Map<String, PaywallWebViewValue> = linkedMapOf(
        KEY_LOCALE to PaywallWebViewValue.String(locale),
    )

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
                        "overwritten.",
                )
            }
            !reserved
        }
        return sanitized
    }
}
