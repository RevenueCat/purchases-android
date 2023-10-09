package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.Offering

internal sealed class OfferingSelection {
    data class OfferingType(val offeringType: Offering) : OfferingSelection()
    data class OfferingId(val offeringId: String) : OfferingSelection()
    object None : OfferingSelection()

    val offering: Offering?
        get() = when (this) {
            is OfferingType -> offeringType
            is OfferingId -> null
            None -> null
        }

    val offeringIdentifier: String?
        get() = when (this) {
            is OfferingType -> offeringType.identifier
            is OfferingId -> offeringId
            None -> null
        }
}

class PaywallViewOptions(builder: Builder) {

    internal val offeringSelection: OfferingSelection
    val shouldDisplayDismissButton: Boolean
    val listener: PaywallViewListener?
    internal var mode: PaywallViewMode = PaywallViewMode.default

    init {
        this.offeringSelection = builder.offeringSelection
        this.shouldDisplayDismissButton = builder.shouldDisplayDismissButton
        this.listener = builder.listener
    }

    class Builder {
        internal var offeringSelection: OfferingSelection = OfferingSelection.None
        internal var shouldDisplayDismissButton: Boolean = false
        internal var listener: PaywallViewListener? = null

        fun setOffering(offering: Offering?) = apply {
            this.offeringSelection = offering?.let { OfferingSelection.OfferingType(it) }
                ?: OfferingSelection.None
        }

        internal fun setOfferingId(offeringId: String?) = apply {
            this.offeringSelection = offeringId?.let { OfferingSelection.OfferingId(it) }
                ?: OfferingSelection.None
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
