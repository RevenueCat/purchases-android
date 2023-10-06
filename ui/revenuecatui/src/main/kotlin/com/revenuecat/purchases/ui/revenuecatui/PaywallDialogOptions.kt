package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering

class PaywallDialogOptions(builder: Builder) {

    val dismissRequest: () -> Unit
    val shouldDisplayBlock: ((CustomerInfo) -> Boolean)?
    val offering: Offering?
    val shouldDisplayDismissButton: Boolean
    val listener: PaywallViewListener?

    init {
        this.shouldDisplayBlock = builder.shouldDisplayBlock
        this.dismissRequest = builder.dismissRequest
        this.offering = builder.offering
        this.shouldDisplayDismissButton = builder.shouldDisplayDismissButton
        this.listener = builder.listener
    }

    fun toPaywallViewOptions(): PaywallViewOptions {
        return PaywallViewOptions.Builder()
            .setOffering(offering)
            .setShouldDisplayDismissButton(shouldDisplayDismissButton)
            .setListener(listener)
            .build()
    }

    class Builder(
        val dismissRequest: () -> Unit,
    ) {
        internal var shouldDisplayBlock: ((CustomerInfo) -> Boolean)? = null
        internal var offering: Offering? = null
        internal var shouldDisplayDismissButton: Boolean = true
        internal var listener: PaywallViewListener? = null

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
                this.shouldDisplayBlock = { customerInfo ->
                    customerInfo.entitlements[requiredEntitlementIdentifier]?.isActive != true
                }
            }
        }

        fun setOffering(offering: Offering?) = apply {
            this.offering = offering
        }

        fun setShouldDisplayDismissButton(shouldDisplayDismissButton: Boolean) = apply {
            this.shouldDisplayDismissButton = shouldDisplayDismissButton
        }

        fun setListener(listener: PaywallViewListener?) = apply {
            this.listener = listener
        }

        fun build(): PaywallDialogOptions {
            return PaywallDialogOptions(this)
        }
    }
}
