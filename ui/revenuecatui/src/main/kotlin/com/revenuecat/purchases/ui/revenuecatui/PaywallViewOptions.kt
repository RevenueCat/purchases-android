package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.Offering

class PaywallViewOptions(builder: Builder) {

    val offering: Offering?
    val offeringId: String?
    val shouldDisplayDismissButton: Boolean
    val listener: PaywallViewListener?

    init {
        this.offering = builder.offering
        this.offeringId = builder.offeringId
        this.shouldDisplayDismissButton = builder.shouldDisplayDismissButton
        this.listener = builder.listener
    }

    class Builder {
        internal var offering: Offering? = null
        internal var offeringId: String? = null
        internal var shouldDisplayDismissButton: Boolean = false
        internal var listener: PaywallViewListener? = null

        fun setOffering(offering: Offering?) = apply {
            this.offering = offering
        }

        internal fun setOfferingId(offeringId: String?) = apply {
            this.offeringId = offeringId
        }

        fun setShouldDisplayDismissButton(shouldDisplayDismissButton: Boolean) = apply {
            this.shouldDisplayDismissButton = shouldDisplayDismissButton
        }

        fun setListener(listener: PaywallViewListener?) = apply {
            this.listener = listener
        }

        fun build(): PaywallViewOptions {
            return PaywallViewOptions(this)
        }
    }
}
