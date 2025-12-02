package com.revenuecat.purchases.ui.revenuecatui

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Stable
internal sealed class OfferingSelection {

    @Immutable
    data class OfferingType(val offeringType: Offering) : OfferingSelection()

    @Parcelize
    @Immutable
    data class IdAndPresentedOfferingContext(
        val offeringId: String,
        val presentedOfferingContext: PresentedOfferingContext?,
    ) : Parcelable, OfferingSelection()

    @Immutable
    object None : OfferingSelection()

    val offering: Offering?
        get() = when (this) {
            is OfferingType -> offeringType
            is IdAndPresentedOfferingContext -> null
            None -> null
        }

    val offeringIdentifier: String?
        get() = when (this) {
            is OfferingType -> offeringType.identifier
            is IdAndPresentedOfferingContext -> offeringId
            None -> null
        }
}

@Poko
@Immutable
class PaywallOptions internal constructor(
    internal val offeringSelection: OfferingSelection,
    internal val shouldDisplayDismissButton: Boolean,
    val fontProvider: FontProvider?,
    val listener: PaywallListener?,
    val purchaseLogic: PurchaseLogic?,
    internal val mode: PaywallMode,
    val dismissRequest: () -> Unit,
    val replaceProductData: ReplaceProductData?,
) {
    companion object {
        private const val hashMultiplier = 31
    }

    constructor(builder: Builder) : this(
        offeringSelection = builder.offeringSelection,
        shouldDisplayDismissButton = builder.shouldDisplayDismissButton,
        fontProvider = builder.fontProvider,
        listener = builder.listener,
        purchaseLogic = builder.purchaseLogic,
        mode = builder.mode,
        dismissRequest = builder.dismissRequest,
        replaceProductData = builder.replaceProductData,
    )

    // Only key fields that affect the paywall's identity and rendering logic are used in hashCode.
    // Fields like fontProvider, listener, purchaseLogic, dismissRequest, and replaceProductData are excluded because
    // they don't influence visual/structural uniqueness and may not be reliably hashable.
    override fun hashCode(): Int {
        var result = offeringSelection.offeringIdentifier.hashCode()
        result = hashMultiplier * result + shouldDisplayDismissButton.hashCode()
        result = hashMultiplier * result + mode.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaywallOptions) return false

        return when {
            this.offeringSelection != other.offeringSelection -> false
            this.shouldDisplayDismissButton != other.shouldDisplayDismissButton -> false
            this.fontProvider != other.fontProvider -> false
            this.listener != other.listener -> false
            this.purchaseLogic != other.purchaseLogic -> false
            this.mode != other.mode -> false
            this.replaceProductData != other.replaceProductData -> false
            else -> this.dismissRequest == other.dismissRequest
        }
    }

    internal fun copy(
        offeringSelection: OfferingSelection = this.offeringSelection,
        shouldDisplayDismissButton: Boolean = this.shouldDisplayDismissButton,
        fontProvider: FontProvider? = this.fontProvider,
        listener: PaywallListener? = this.listener,
        purchaseLogic: PurchaseLogic? = this.purchaseLogic,
        mode: PaywallMode = this.mode,
        dismissRequest: () -> Unit = this.dismissRequest,
        replaceProductData: ReplaceProductData? = this.replaceProductData,
    ): PaywallOptions = PaywallOptions(
        offeringSelection = offeringSelection,
        shouldDisplayDismissButton = shouldDisplayDismissButton,
        fontProvider = fontProvider,
        listener = listener,
        purchaseLogic = purchaseLogic,
        mode = mode,
        dismissRequest = dismissRequest,
        replaceProductData = replaceProductData,
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
        internal var replaceProductData: ReplaceProductData? = null

        fun setOffering(offering: Offering?) = apply {
            this.offeringSelection = offering?.let { OfferingSelection.OfferingType(it) }
                ?: OfferingSelection.None
        }

        internal fun setOfferingIdAndPresentedOfferingContext(
            idAndPresentedOfferingContext: OfferingSelection.IdAndPresentedOfferingContext?,
        ) = apply {
            this.offeringSelection = idAndPresentedOfferingContext ?: OfferingSelection.None
        }

        /**
         * Sets whether to display a close button on the paywall screen. Only available when using
         * [Paywall] and original template paywalls. Ignored when using [OriginalTemplatePaywallFooter] or
         * using v2 Paywalls. Defaults to false.
         */
        fun setShouldDisplayDismissButton(shouldDisplayDismissButton: Boolean) = apply {
            this.shouldDisplayDismissButton = shouldDisplayDismissButton
        }

        /**
         * Sets a font provider to provide the paywall with your custom fonts.
         * Only available for original template paywalls. Ignored for v2 Paywalls.
         */
        fun setFontProvider(fontProvider: FontProvider?) = apply {
            this.fontProvider = fontProvider
        }

        fun setListener(listener: PaywallListener?) = apply {
            this.listener = listener
        }

        fun setPurchaseLogic(purchaseLogic: PurchaseLogic?) = apply {
            this.purchaseLogic = purchaseLogic
        }

        /**
         * Sets [ReplaceProductData] to the builder to start the correct product upgrade flow.
         */
        fun setReplaceProductData(replaceProductData: ReplaceProductData?) = apply {
            this.replaceProductData = replaceProductData
        }

        internal fun setMode(mode: PaywallMode) = apply {
            this.mode = mode
        }

        fun build(): PaywallOptions {
            return PaywallOptions(this)
        }
    }
}
