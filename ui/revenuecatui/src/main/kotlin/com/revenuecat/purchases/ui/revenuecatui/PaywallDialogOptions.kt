package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayBlockForEntitlementIdentifier
import dev.drewhamilton.poko.Poko

@Immutable
@Poko
public class PaywallDialogOptions internal constructor(
    public val shouldDisplayBlock: ((CustomerInfo) -> Boolean)?,
    public val dismissRequest: (() -> Unit)?,
    public val offering: Offering?,
    public val shouldDisplayDismissButton: Boolean,
    public val fontProvider: FontProvider?,
    public val listener: PaywallListener?,
    public val purchaseLogic: PaywallPurchaseLogic?,
    /**
     * Custom variables to be used in paywall text. These values will replace `{{ custom.key }}` or
     * `{{ $custom.key }}` placeholders in the paywall configuration.
     */
    public val customVariables: Map<String, CustomVariableValue> = emptyMap(),
) {

    internal val offeringSelection: OfferingSelection
        get() = offering?.let { OfferingSelection.OfferingType(it) } ?: OfferingSelection.None

    public constructor(builder: Builder) : this(
        shouldDisplayBlock = builder.shouldDisplayBlock,
        dismissRequest = builder.dismissRequest,
        offering = builder.offering,
        shouldDisplayDismissButton = builder.shouldDisplayDismissButton,
        fontProvider = builder.fontProvider,
        listener = builder.listener,
        purchaseLogic = builder.purchaseLogic,
        customVariables = builder.customVariables,
    )

    public class Builder {
        internal var shouldDisplayBlock: ((CustomerInfo) -> Boolean)? = null
        internal var dismissRequest: (() -> Unit)? = null
        internal var offering: Offering? = null
        internal var shouldDisplayDismissButton: Boolean = true
        internal var fontProvider: FontProvider? = null
        internal var listener: PaywallListener? = null
        internal var purchaseLogic: PaywallPurchaseLogic? = null
        internal var customVariables: Map<String, CustomVariableValue> = emptyMap()

        /**
         * Allows to configure whether to display the paywall dialog depending on operations on the CustomerInfo
         */
        public fun setShouldDisplayBlock(shouldDisplayBlock: ((CustomerInfo) -> Boolean)?): Builder = apply {
            this.shouldDisplayBlock = shouldDisplayBlock
        }

        /**
         * Allows to configure whether to display the paywall dialog depending on the presence of a specific entitlement
         */
        public fun setRequiredEntitlementIdentifier(requiredEntitlementIdentifier: String?): Builder = apply {
            requiredEntitlementIdentifier?.let { requiredEntitlementIdentifier ->
                this.shouldDisplayBlock = shouldDisplayBlockForEntitlementIdentifier(requiredEntitlementIdentifier)
            }
        }

        public fun setDismissRequest(dismissRequest: () -> Unit): Builder = apply {
            this.dismissRequest = dismissRequest
        }

        public fun setOffering(offering: Offering?): Builder = apply {
            this.offering = offering
        }

        /**
         * Sets whether to display a close button on the paywall screen. Only available for original template paywalls.
         * Ignored for v2 Paywalls. Defaults to true.
         */
        public fun setShouldDisplayDismissButton(shouldDisplayDismissButton: Boolean): Builder = apply {
            this.shouldDisplayDismissButton = shouldDisplayDismissButton
        }

        /**
         * Sets a font provider to provide the paywall with your custom fonts.
         * Only available for original template paywalls. Ignored for v2 Paywalls.
         */
        public fun setFontProvider(fontProvider: FontProvider?): Builder = apply {
            this.fontProvider = fontProvider
        }

        public fun setListener(listener: PaywallListener?): Builder = apply {
            this.listener = listener
        }

        public fun setCustomPurchaseLogic(purchaseLogic: PaywallPurchaseLogic?): Builder = apply {
            this.purchaseLogic = purchaseLogic
        }

        /**
         * Sets custom variables to be used in paywall text. These values will replace
         * `{{ custom.key }}` or `{{ $custom.key }}` placeholders in the paywall configuration.
         *
         * @param variables A map of variable names to their [CustomVariableValue] values.
         */
        public fun setCustomVariables(variables: Map<String, CustomVariableValue>): Builder = apply {
            this.customVariables = variables
        }

        public fun build(): PaywallDialogOptions {
            return PaywallDialogOptions(this)
        }
    }
}
