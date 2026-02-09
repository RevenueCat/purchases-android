package com.revenuecat.purchases.ui.revenuecatui.activity

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableKeyValidator
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider

/**
 * Configuration options for launching a paywall activity.
 *
 * Use the [Builder] to create an instance of this class:
 * ```kotlin
 * val options = PaywallActivityLaunchOptions.Builder()
 *     .setOffering(offering)
 *     .setCustomVariables(mapOf("user_name" to CustomVariableValue.String("John")))
 *     .setShouldDisplayDismissButton(true)
 *     .build()
 *
 * launcher.launch(options)
 * ```
 *
 * For conditional launches, you can also set:
 * - [requiredEntitlementIdentifier] to only show the paywall if the user doesn't have that entitlement
 * - [shouldDisplayBlock] to provide custom logic for when to show the paywall
 */
@Suppress("LongParameterList")
class PaywallActivityLaunchOptions private constructor(
    internal val offering: Offering?,
    internal val fontProvider: ParcelizableFontProvider?,
    internal val shouldDisplayDismissButton: Boolean,
    internal val edgeToEdge: Boolean,
    internal val customVariables: Map<String, CustomVariableValue>,
    internal val requiredEntitlementIdentifier: String?,
    internal val shouldDisplayBlock: ((CustomerInfo) -> Boolean)?,
    internal val paywallDisplayCallback: PaywallDisplayCallback?,
) {
    /**
     * Builder for creating [PaywallActivityLaunchOptions].
     */
    class Builder {
        private var offering: Offering? = null
        private var fontProvider: ParcelizableFontProvider? = null
        private var shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON
        private var edgeToEdge: Boolean = defaultEdgeToEdge
        private var customVariables: Map<String, CustomVariableValue> = emptyMap()
        private var requiredEntitlementIdentifier: String? = null
        private var shouldDisplayBlock: ((CustomerInfo) -> Boolean)? = null
        private var paywallDisplayCallback: PaywallDisplayCallback? = null

        /**
         * Sets the offering to be shown in the paywall.
         * If not set, the current offering will be shown.
         */
        fun setOffering(offering: Offering?) = apply {
            this.offering = offering
        }

        /**
         * Sets the font provider to be used in the paywall.
         * Only available for original template paywalls. Ignored for V2 Paywalls.
         */
        fun setFontProvider(fontProvider: ParcelizableFontProvider?) = apply {
            this.fontProvider = fontProvider
        }

        /**
         * Sets whether to display the dismiss button in the paywall.
         * Only available for original template paywalls. Ignored for V2 Paywalls.
         * Default is true.
         */
        fun setShouldDisplayDismissButton(shouldDisplayDismissButton: Boolean) = apply {
            this.shouldDisplayDismissButton = shouldDisplayDismissButton
        }

        /**
         * Sets whether to display the paywall in edge-to-edge mode.
         * Default is true for Android 15+, false otherwise.
         */
        fun setEdgeToEdge(edgeToEdge: Boolean) = apply {
            this.edgeToEdge = edgeToEdge
        }

        /**
         * Sets custom variables to be used in paywall text.
         * These values will replace `{{ custom.key }}` or `{{ $custom.key }}` placeholders
         * in the paywall configuration.
         *
         * Invalid keys (those not starting with a letter or containing invalid characters)
         * will be filtered out and logged as warnings.
         */
        fun setCustomVariables(customVariables: Map<String, CustomVariableValue>) = apply {
            this.customVariables = CustomVariableKeyValidator.validateAndFilter(customVariables)
        }

        /**
         * Sets the required entitlement identifier.
         * When set, the paywall will only be displayed if the current user does not have
         * this entitlement active.
         *
         * This is mutually exclusive with [setShouldDisplayBlock].
         */
        fun setRequiredEntitlementIdentifier(requiredEntitlementIdentifier: String?) = apply {
            this.requiredEntitlementIdentifier = requiredEntitlementIdentifier
        }

        /**
         * Sets a block that determines whether the paywall should be displayed.
         * The paywall will be displayed only if this returns true.
         *
         * This is mutually exclusive with [setRequiredEntitlementIdentifier].
         */
        fun setShouldDisplayBlock(shouldDisplayBlock: ((CustomerInfo) -> Boolean)?) = apply {
            this.shouldDisplayBlock = shouldDisplayBlock
        }

        /**
         * Sets a callback that will be called with the result of whether the paywall was displayed.
         * Only applicable when using [setRequiredEntitlementIdentifier].
         */
        fun setPaywallDisplayCallback(paywallDisplayCallback: PaywallDisplayCallback?) = apply {
            this.paywallDisplayCallback = paywallDisplayCallback
        }

        /**
         * Builds the [PaywallActivityLaunchOptions] instance.
         */
        fun build(): PaywallActivityLaunchOptions {
            return PaywallActivityLaunchOptions(
                offering = offering,
                fontProvider = fontProvider,
                shouldDisplayDismissButton = shouldDisplayDismissButton,
                edgeToEdge = edgeToEdge,
                customVariables = customVariables,
                requiredEntitlementIdentifier = requiredEntitlementIdentifier,
                shouldDisplayBlock = shouldDisplayBlock,
                paywallDisplayCallback = paywallDisplayCallback,
            )
        }
    }
}
