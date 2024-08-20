package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider

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

data class PaywallOptions internal constructor(
    internal val offeringSelection: OfferingSelection,
    internal val shouldDisplayDismissButton: Boolean,
    val fontProvider: FontProvider?,
    val listener: PaywallListener?,
    val purchaseLogic: PurchaseLogic?,
    internal val mode: PaywallMode,
    val dismissRequest: () -> Unit,
) {

    constructor(builder: Builder) : this(
        offeringSelection = builder.offeringSelection,
        shouldDisplayDismissButton = builder.shouldDisplayDismissButton,
        fontProvider = builder.fontProvider,
        listener = builder.listener,
        purchaseLogic = builder.purchaseLogic,
        mode = builder.mode,
        dismissRequest = builder.dismissRequest,
    )

    class Builder(
        internal val dismissRequest: () -> Unit,
    ) {
        internal var offeringSelection: OfferingSelection = OfferingSelection.None
        internal var shouldDisplayDismissButton: Boolean = false
        internal var fontProvider: FontProvider? = null
        internal var listener: PaywallListener? = null
        internal var purchaseLogic: PurchaseLogic? = null
        internal var mode: PaywallMode = PaywallMode.default

        fun setOffering(offering: Offering?) = apply {
            this.offeringSelection = offering?.let { OfferingSelection.OfferingType(it) }
                ?: OfferingSelection.None
        }

        internal fun setOfferingId(offeringId: String?) = apply {
            this.offeringSelection = offeringId?.let { OfferingSelection.OfferingId(it) }
                ?: OfferingSelection.None
        }

        /**
         * Sets whether to display a close button on the paywall screen. Only available when using
         * [Paywall]. Ignored when using [PaywallFooter]. Defaults to false.
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

        fun setPurchaseLogic(purchaseLogic: PurchaseLogic?) = apply {
            this.purchaseLogic = purchaseLogic
        }

        internal fun setMode(mode: PaywallMode) = apply {
            this.mode = mode
        }

        fun build(): PaywallOptions {
            return PaywallOptions(this)
        }
    }
}
