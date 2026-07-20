@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

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
 */
internal object PaywallWebViewVariablesProvider {

    const val KEY_LOCALE: String = "locale"

    /**
     * The SDK-managed system variables exposed to the web view.
     *
     * @param locale a BCP-47-ish locale string, e.g. `en-US`.
     */
    fun sdkManagedVariables(
        locale: String,
    ): Map<String, String> = mapOf(
        KEY_LOCALE to locale,
    )
}
