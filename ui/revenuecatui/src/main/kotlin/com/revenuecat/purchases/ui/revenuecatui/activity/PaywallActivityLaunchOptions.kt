package com.revenuecat.purchases.ui.revenuecatui.activity

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableKeyValidator
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider

/**
 * Configuration options for launching a paywall activity unconditionally.
 *
 * Use the [Builder] to create an instance of this class:
 * ```kotlin
 * val options = PaywallActivityLaunchOptions.Builder()
 *     .setOffering(offering)
 *     .setCustomVariables(mapOf("user_name" to CustomVariableValue.String("John")))
 *     .setShouldDisplayDismissButton(true)
 *     .build()
 *
 * launcher.launchWithOptions(options)
 * ```
 *
 * For conditional launches (showing the paywall only when certain conditions are met),
 * use [PaywallActivityLaunchIfNeededOptions] instead.
 */
@Suppress("LongParameterList")
class PaywallActivityLaunchOptions private constructor(
    internal val offering: Offering?,
    internal val fontProvider: ParcelizableFontProvider?,
    internal val shouldDisplayDismissButton: Boolean,
    internal val edgeToEdge: Boolean,
    internal val customVariables: Map<String, CustomVariableValue>,
    // Internal properties for hybrid SDK support
    internal val offeringIdentifier: String?,
    internal val presentedOfferingContext: PresentedOfferingContext?,
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

        // Internal properties for hybrid SDK support
        private var offeringIdentifier: String? = null
        private var presentedOfferingContext: PresentedOfferingContext? = null

        /**
         * Sets the offering to be shown in the paywall.
         * If not set, the current offering will be shown.
         */
        fun setOffering(offering: Offering?) = apply {
            this.offering = offering
            // Clear internal offering fields when using public API
            this.offeringIdentifier = null
            this.presentedOfferingContext = null
        }

        /**
         * Internal method for hybrid SDKs to set offering by identifier and context.
         * This is mutually exclusive with [setOffering].
         */
        @InternalRevenueCatAPI
        fun setOfferingIdentifier(
            offeringIdentifier: String,
            presentedOfferingContext: PresentedOfferingContext,
        ) = apply {
            this.offeringIdentifier = offeringIdentifier
            this.presentedOfferingContext = presentedOfferingContext
            // Clear public offering when using internal API
            this.offering = null
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
         * Builds the [PaywallActivityLaunchOptions] instance.
         */
        fun build(): PaywallActivityLaunchOptions {
            return PaywallActivityLaunchOptions(
                offering = offering,
                fontProvider = fontProvider,
                shouldDisplayDismissButton = shouldDisplayDismissButton,
                edgeToEdge = edgeToEdge,
                customVariables = customVariables,
                offeringIdentifier = offeringIdentifier,
                presentedOfferingContext = presentedOfferingContext,
            )
        }
    }
}

/**
 * Configuration options for conditionally launching a paywall activity.
 *
 * This class requires a display condition to be set - either [requiredEntitlementIdentifier]
 * or [shouldDisplayBlock]. The paywall will only be shown when the condition is met.
 *
 * Use the [Builder] to create an instance:
 *
 * Example with entitlement check:
 * ```kotlin
 * val options = PaywallActivityLaunchIfNeededOptions.Builder()
 *     .setRequiredEntitlementIdentifier("premium")
 *     .setOffering(offering)
 *     .setCustomVariables(mapOf("user_name" to CustomVariableValue.String("John")))
 *     .setPaywallDisplayCallback(callback)
 *     .build()
 *
 * launcher.launchIfNeededWithOptions(options)
 * ```
 *
 * Example with custom condition:
 * ```kotlin
 * val options = PaywallActivityLaunchIfNeededOptions.Builder()
 *     .setShouldDisplayBlock { customerInfo ->
 *         customerInfo.entitlements.active.isEmpty()
 *     }
 *     .setOffering(offering)
 *     .build()
 *
 * launcher.launchIfNeededWithOptions(options)
 * ```
 */
@Suppress("LongParameterList")
class PaywallActivityLaunchIfNeededOptions private constructor(
    internal val offering: Offering?,
    internal val fontProvider: ParcelizableFontProvider?,
    internal val shouldDisplayDismissButton: Boolean,
    internal val edgeToEdge: Boolean,
    internal val customVariables: Map<String, CustomVariableValue>,
    internal val requiredEntitlementIdentifier: String?,
    internal val shouldDisplayBlock: ((CustomerInfo) -> Boolean)?,
    internal val paywallDisplayCallback: PaywallDisplayCallback?,
    // Internal properties for hybrid SDK support
    internal val offeringIdentifier: String?,
    internal val presentedOfferingContext: PresentedOfferingContext?,
) {
    /**
     * Builder for creating [PaywallActivityLaunchIfNeededOptions].
     *
     * You must set either [setRequiredEntitlementIdentifier] or [setShouldDisplayBlock]
     * before calling [build]. These are mutually exclusive - setting one will clear the other.
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

        // Internal properties for hybrid SDK support
        private var offeringIdentifier: String? = null
        private var presentedOfferingContext: PresentedOfferingContext? = null

        /**
         * Sets the offering to be shown in the paywall.
         * If not set, the current offering will be shown.
         */
        fun setOffering(offering: Offering?) = apply {
            this.offering = offering
            // Clear internal offering fields when using public API
            this.offeringIdentifier = null
            this.presentedOfferingContext = null
        }

        /**
         * Internal method for hybrid SDKs to set offering by identifier and context.
         * This is mutually exclusive with [setOffering].
         */
        @InternalRevenueCatAPI
        fun setOfferingIdentifier(
            offeringIdentifier: String,
            presentedOfferingContext: PresentedOfferingContext,
        ) = apply {
            this.offeringIdentifier = offeringIdentifier
            this.presentedOfferingContext = presentedOfferingContext
            // Clear public offering when using internal API
            this.offering = null
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
         * The paywall will only be displayed if the current user does not have
         * this entitlement active.
         *
         * This is mutually exclusive with [setShouldDisplayBlock]. Setting this will clear
         * any previously set shouldDisplayBlock.
         */
        fun setRequiredEntitlementIdentifier(requiredEntitlementIdentifier: String) = apply {
            this.requiredEntitlementIdentifier = requiredEntitlementIdentifier
            this.shouldDisplayBlock = null
        }

        /**
         * Sets a block that determines whether the paywall should be displayed.
         * The paywall will be displayed only if this returns true.
         *
         * This is mutually exclusive with [setRequiredEntitlementIdentifier]. Setting this
         * will clear any previously set requiredEntitlementIdentifier.
         */
        fun setShouldDisplayBlock(shouldDisplayBlock: (CustomerInfo) -> Boolean) = apply {
            this.shouldDisplayBlock = shouldDisplayBlock
            this.requiredEntitlementIdentifier = null
        }

        /**
         * Sets a callback that will be called with the result of whether the paywall was displayed.
         */
        fun setPaywallDisplayCallback(paywallDisplayCallback: PaywallDisplayCallback?) = apply {
            this.paywallDisplayCallback = paywallDisplayCallback
        }

        /**
         * Builds the [PaywallActivityLaunchIfNeededOptions] instance.
         *
         * @throws IllegalStateException if neither [setRequiredEntitlementIdentifier] nor
         * [setShouldDisplayBlock] has been called.
         */
        fun build(): PaywallActivityLaunchIfNeededOptions {
            require(requiredEntitlementIdentifier != null || shouldDisplayBlock != null) {
                "PaywallActivityLaunchIfNeededOptions requires either requiredEntitlementIdentifier " +
                    "or shouldDisplayBlock to be set. Use PaywallActivityLaunchOptions for " +
                    "unconditional launches."
            }
            return PaywallActivityLaunchIfNeededOptions(
                offering = offering,
                fontProvider = fontProvider,
                shouldDisplayDismissButton = shouldDisplayDismissButton,
                edgeToEdge = edgeToEdge,
                customVariables = customVariables,
                requiredEntitlementIdentifier = requiredEntitlementIdentifier,
                shouldDisplayBlock = shouldDisplayBlock,
                paywallDisplayCallback = paywallDisplayCallback,
                offeringIdentifier = offeringIdentifier,
                presentedOfferingContext = presentedOfferingContext,
            )
        }
    }
}
