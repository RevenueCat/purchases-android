package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.Offering

class PaywallFooterOptions(builder: Builder) {

    val isVisible: Boolean
    val shouldAllowDismissing: Boolean
    val condensed: Boolean
    val offering: Offering?
    val listener: PaywallViewListener?

    init {
        this.isVisible = builder.isVisible
        this.shouldAllowDismissing = builder.shouldAllowDismissing
        this.condensed = builder.condensed
        this.offering = builder.offering
        this.listener = builder.listener
    }

    class Builder {
        internal var isVisible: Boolean = true
        internal var shouldAllowDismissing: Boolean = false
        internal var condensed: Boolean = false
        internal var offering: Offering? = null
        internal var listener: PaywallViewListener? = null

        fun setIsVisible(isVisible: Boolean) = apply {
            this.isVisible = isVisible
        }

        fun setShouldAllowDismissing(shouldAllowDismissing: Boolean) = apply {
            this.shouldAllowDismissing = shouldAllowDismissing
        }

        fun setCondensed(condensed: Boolean) = apply {
            this.condensed = condensed
        }

        fun setOffering(offering: Offering?) = apply {
            this.offering = offering
        }

        fun setListener(listener: PaywallViewListener) = apply {
            this.listener = listener
        }

        fun build(): PaywallFooterOptions {
            return PaywallFooterOptions(this)
        }
    }
}
