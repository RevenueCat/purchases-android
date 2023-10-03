package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.Offering

class PaywallViewOptions(builder: Builder) {

    val offering: Offering?
    val shouldDisplayDismissButton: Boolean
    val listener: PaywallViewListener?

    init {
        this.offering = builder.offering
        this.shouldDisplayDismissButton = builder.shouldDisplayDismissButton
        this.listener = builder.listener
    }

    class Builder {
        internal var offering: Offering? = null
        internal var shouldDisplayDismissButton: Boolean = false
        internal var listener: PaywallViewListener? = null

        fun setOffering(offering: Offering?) = apply {
            this.offering = offering
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
