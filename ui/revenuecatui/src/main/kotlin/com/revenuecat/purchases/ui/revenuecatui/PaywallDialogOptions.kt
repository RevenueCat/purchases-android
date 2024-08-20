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
    val myAppPurchaseLogic: MyAppPurchaseLogic?,
) {

    constructor(builder: Builder) : this(
        shouldDisplayBlock = builder.shouldDisplayBlock,
        dismissRequest = builder.dismissRequest,
        offering = builder.offering,
        shouldDisplayDismissButton = builder.shouldDisplayDismissButton,
        fontProvider = builder.fontProvider,
        listener = builder.listener,
        myAppPurchaseLogic = builder.myAppPurchaseLogic,
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
            .setMyAppPurchaseLogic(myAppPurchaseLogic)
            .build()
    }

    class Builder {
        internal var shouldDisplayBlock: ((CustomerInfo) -> Boolean)? = null
        internal var dismissRequest: (() -> Unit)? = null
        internal var offering: Offering? = null
        internal var shouldDisplayDismissButton: Boolean = true
        internal var fontProvider: FontProvider? = null
        internal var listener: PaywallListener? = null
        internal var myAppPurchaseLogic: MyAppPurchaseLogic? = null

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

        fun setMyAppPurchaseLogic(myAppPurchaseLogic: MyAppPurchaseLogic?) = apply {
            this.myAppPurchaseLogic = myAppPurchaseLogic
        }

        fun build(): PaywallDialogOptions {
            return PaywallDialogOptions(this)
        }
    }
}
