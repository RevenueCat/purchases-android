package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.Offering

class PaywallViewOptions(builder: Builder) {

    val offering: Offering?
    val shouldDisplayDismissButton: Boolean
    val listener: PaywallViewListener?
    internal val mode: PaywallViewMode

    init {
        this.offering = builder.offering
        this.shouldDisplayDismissButton = builder.shouldDisplayDismissButton
        this.listener = builder.listener
        this.mode = builder.mode
    }

    internal fun changeMode(mode: PaywallViewMode): PaywallViewOptions {
        return Builder()
            .setOffering(offering)
            .setListener(listener)
            .setShouldDisplayDismissButton(shouldDisplayDismissButton)
            .setMode(mode)
            .build()
    }

    class Builder {
        internal var offering: Offering? = null
        internal var shouldDisplayDismissButton: Boolean = false
        internal var listener: PaywallViewListener? = null
        internal var mode: PaywallViewMode = PaywallViewMode.default

        fun setOffering(offering: Offering?) = apply {
            this.offering = offering
        }

        fun setShouldDisplayDismissButton(shouldDisplayDismissButton: Boolean) = apply {
            this.shouldDisplayDismissButton = shouldDisplayDismissButton
        }

        fun setListener(listener: PaywallViewListener?) = apply {
            this.listener = listener
        }

        internal fun setMode(mode: PaywallViewMode) = apply {
            this.mode = mode
        }

        fun build(): PaywallViewOptions {
            return PaywallViewOptions(this)
        }
    }
}
