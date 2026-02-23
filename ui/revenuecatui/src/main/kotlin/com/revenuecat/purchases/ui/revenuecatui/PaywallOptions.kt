@file:Suppress("DEPRECATION")

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
public class PaywallOptions internal constructor(
    internal val offeringSelection: OfferingSelection,
    internal val shouldDisplayDismissButton: Boolean,
    public val fontProvider: FontProvider?,
    public val listener: PaywallListener?,
    public val purchaseLogic: PurchaseLogic?,
    internal val mode: PaywallMode,
    public val dismissRequest: () -> Unit,
    internal val dismissRequestWithExitOffering: ((exitOffering: Offering?) -> Unit)? = null,
    /**
     * Custom variables to be used in paywall text. These values will replace `{{ custom.key }}` or
     * `{{ $custom.key }}` placeholders in the paywall configuration.
     */
    public val customVariables: Map<String, CustomVariableValue> = emptyMap(),
) {
    public companion object {
        private const val hashMultiplier = 31
    }

    public constructor(builder: Builder) : this(
        offeringSelection = builder.offeringSelection,
        shouldDisplayDismissButton = builder.shouldDisplayDismissButton,
        fontProvider = builder.fontProvider,
        listener = builder.listener,
        purchaseLogic = builder.purchaseLogic,
        mode = builder.mode,
        dismissRequest = builder.dismissRequest,
        dismissRequestWithExitOffering = builder.dismissRequestWithExitOffering,
        customVariables = builder.customVariables,
    )

    // Only key fields that affect the paywall's identity and rendering logic are used in hashCode.
    // Fields like fontProvider, listener, purchaseLogic, and dismissRequest are excluded because
    // they don't influence visual/structural uniqueness and may not be reliably hashable.
    override fun hashCode(): Int {
        var result = offeringSelection.offeringIdentifier.hashCode()
        result = hashMultiplier * result + shouldDisplayDismissButton.hashCode()
        result = hashMultiplier * result + mode.hashCode()
        result = hashMultiplier * result + customVariables.hashCode()
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
            this.customVariables != other.customVariables -> false
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
        dismissRequestWithExitOffering: ((Offering?) -> Unit)? = this.dismissRequestWithExitOffering,
        customVariables: Map<String, CustomVariableValue> = this.customVariables,
    ): PaywallOptions = PaywallOptions(
        offeringSelection = offeringSelection,
        shouldDisplayDismissButton = shouldDisplayDismissButton,
        fontProvider = fontProvider,
        listener = listener,
        purchaseLogic = purchaseLogic,
        mode = mode,
        dismissRequest = dismissRequest,
        dismissRequestWithExitOffering = dismissRequestWithExitOffering,
        customVariables = customVariables,
    )

    @Suppress("TooManyFunctions")
    public class Builder(
        internal val dismissRequest: () -> Unit,
    ) {
        internal var offeringSelection: OfferingSelection = OfferingSelection.None
        internal var shouldDisplayDismissButton: Boolean = false
        internal var fontProvider: FontProvider? = null
        internal var listener: PaywallListener? = null
        internal var purchaseLogic: PurchaseLogic? = null
        internal var mode: PaywallMode = PaywallMode.default
        internal var dismissRequestWithExitOffering: ((Offering?) -> Unit)? = null
        internal var customVariables: Map<String, CustomVariableValue> = emptyMap()

        public fun setOffering(offering: Offering?): Builder = apply {
            this.offeringSelection = offering?.let { OfferingSelection.OfferingType(it) }
                ?: OfferingSelection.None
        }

        internal fun setOfferingIdAndPresentedOfferingContext(
            idAndPresentedOfferingContext: OfferingSelection.IdAndPresentedOfferingContext?,
        ) = apply {
            this.offeringSelection = idAndPresentedOfferingContext ?: OfferingSelection.None
        }

        internal fun setOfferingSelection(offeringSelection: OfferingSelection?) = apply {
            this.offeringSelection = offeringSelection ?: OfferingSelection.None
        }

        /**
         * Sets whether to display a close button on the paywall screen. Only available when using
         * [Paywall] and original template paywalls. Ignored when using [OriginalTemplatePaywallFooter] or
         * using v2 Paywalls. Defaults to false.
         */
        public fun setShouldDisplayDismissButton(shouldDisplayDismissButton: Boolean): Builder = apply {
            this.shouldDisplayDismissButton = shouldDisplayDismissButton
        }

        /**
         * Sets a font provider to provide the paywall with your custom fonts.
         * Only available for original template paywalls. Ignored for v2 Paywalls.
         */
        public fun setFontProvider(fontProvider: FontProvider?): Builder = apply {
            this.fontProvider = fontProvider
        }

        public fun setListener(listener: PaywallListener?): Builder = apply {
            this.listener = listener
        }

        public fun setPurchaseLogic(purchaseLogic: PurchaseLogic?): Builder = apply {
            this.purchaseLogic = purchaseLogic
        }

        internal fun setMode(mode: PaywallMode) = apply {
            this.mode = mode
        }

        internal fun setDismissRequestWithExitOffering(dismissRequestWithExitOffering: ((Offering?) -> Unit)?) = apply {
            this.dismissRequestWithExitOffering = dismissRequestWithExitOffering
        }

        /**
         * Sets custom variables to be used in paywall text. These values will replace
         * `{{ custom.key }}` or `{{ $custom.key }}` placeholders in the paywall configuration.
         *
         * @param variables A map of variable names to their [CustomVariableValue] values.
         */
        public fun setCustomVariables(variables: Map<String, CustomVariableValue>): Builder = apply {
            this.customVariables = CustomVariableKeyValidator.validateAndFilter(variables)
        }

        public fun build(): PaywallOptions {
            return PaywallOptions(this)
        }
    }
}
