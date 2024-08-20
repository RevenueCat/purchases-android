package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayBlockForEntitlementIdentifier

data class PaywallDialogOptions internal constructor(
    val shouldDisplayBlock: ((CustomerInfo) -> Boolean)?,
    val dismissRequest: (() -> Unit)?,
    val offering: Offering?,
    val shouldDisplayDismissButton: Boolean,
    val fontProvider: FontProvider?,
    val listener: PaywallListener?,
    val purchaseLogic: PurchaseLogic?,
) {

    constructor(builder: Builder) : this(
        shouldDisplayBlock = builder.shouldDisplayBlock,
        dismissRequest = builder.dismissRequest,
        offering = builder.offering,
        shouldDisplayDismissButton = builder.shouldDisplayDismissButton,
        fontProvider = builder.fontProvider,
        listener = builder.listener,
        purchaseLogic = builder.purchaseLogic,
    )

    internal fun toPaywallOptions(dismissRequest: () -> Unit): PaywallOptions {
        return PaywallOptions.Builder {
            dismissRequest()
            this.dismissRequest?.invoke()
        }
            .setOffering(offering)
            .setShouldDisplayDismissButton(shouldDisplayDismissButton)
            .setFontProvider(fontProvider)
            .setListener(listener)
            .setPurchaseLogic(purchaseLogic)
            .build()
    }

    class Builder {
        internal var shouldDisplayBlock: ((CustomerInfo) -> Boolean)? = null
        internal var dismissRequest: (() -> Unit)? = null
        internal var offering: Offering? = null
        internal var shouldDisplayDismissButton: Boolean = true
        internal var fontProvider: FontProvider? = null
        internal var listener: PaywallListener? = null
        internal var purchaseLogic: PurchaseLogic? = null

        /**
         * Allows to configure whether to display the paywall dialog depending on operations on the CustomerInfo
         */
        fun setShouldDisplayBlock(shouldDisplayBlock: ((CustomerInfo) -> Boolean)?) = apply {
            this.shouldDisplayBlock = shouldDisplayBlock
        }

        /**
         * Allows to configure whether to display the paywall dialog depending on the presence of a specific entitlement
         */
        fun setRequiredEntitlementIdentifier(requiredEntitlementIdentifier: String?) = apply {
            requiredEntitlementIdentifier?.let { requiredEntitlementIdentifier ->
                this.shouldDisplayBlock = shouldDisplayBlockForEntitlementIdentifier(requiredEntitlementIdentifier)
            }
        }

        fun setDismissRequest(dismissRequest: () -> Unit) = apply {
            this.dismissRequest = dismissRequest
        }

        fun setOffering(offering: Offering?) = apply {
            this.offering = offering
        }

        /**
         * Sets whether to display a close button on the paywall screen. Defaults to true.
         */
        fun setShouldDisplayDismissButton(shouldDisplayDismissButton: Boolean) = apply {
            this.shouldDisplayDismissButton = shouldDisplayDismissButton
        }

        fun setFontProvider(fontProvider: FontProvider?) = apply {
            this.fontProvider = fontProvider
        }

        fun setListener(listener: PaywallListener?) = apply {
            this.listener = listener
        }

        fun AppPurchaseLogic(purchaseLogic: PurchaseLogic?) = apply {
            this.purchaseLogic = purchaseLogic
        }

        fun build(): PaywallDialogOptions {
            return PaywallDialogOptions(this)
        }
    }
}
