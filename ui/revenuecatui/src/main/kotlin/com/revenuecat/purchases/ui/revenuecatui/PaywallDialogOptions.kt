package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayBlockForEntitlementIdentifier
import dev.drewhamilton.poko.Poko

@Immutable
@Poko
public class PaywallDialogOptions internal constructor(
    public val shouldDisplayBlock: ((CustomerInfo) -> Boolean)?,
    public val dismissRequest: (() -> Unit)?,
    public val offering: Offering?,
    public val shouldDisplayDismissButton: Boolean,
    public val fontProvider: FontProvider?,
    public val listener: PaywallListener?,
    public val purchaseLogic: PurchaseLogic?,
) {

    public constructor(builder: Builder) : this(
        shouldDisplayBlock = builder.shouldDisplayBlock,
        dismissRequest = builder.dismissRequest,
        offering = builder.offering,
        shouldDisplayDismissButton = builder.shouldDisplayDismissButton,
        fontProvider = builder.fontProvider,
        listener = builder.listener,
        purchaseLogic = builder.purchaseLogic,
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
            .setPurchaseLogic(purchaseLogic)
            .build()
    }

    public class Builder {
        internal var shouldDisplayBlock: ((CustomerInfo) -> Boolean)? = null
        internal var dismissRequest: (() -> Unit)? = null
        internal var offering: Offering? = null
        internal var shouldDisplayDismissButton: Boolean = true
        internal var fontProvider: FontProvider? = null
        internal var listener: PaywallListener? = null
        internal var purchaseLogic: PurchaseLogic? = null

        /**
         * Allows to configure whether to display the paywall dialog depending on operations on the CustomerInfo
         */
        public fun setShouldDisplayBlock(shouldDisplayBlock: ((CustomerInfo) -> Boolean)?): Builder = apply {
            this.shouldDisplayBlock = shouldDisplayBlock
        }

        /**
         * Allows to configure whether to display the paywall dialog depending on the presence of a specific entitlement
         */
        public fun setRequiredEntitlementIdentifier(requiredEntitlementIdentifier: String?): Builder = apply {
            requiredEntitlementIdentifier?.let { requiredEntitlementIdentifier ->
                this.shouldDisplayBlock = shouldDisplayBlockForEntitlementIdentifier(requiredEntitlementIdentifier)
            }
        }

        public fun setDismissRequest(dismissRequest: () -> Unit): Builder = apply {
            this.dismissRequest = dismissRequest
        }

        public fun setOffering(offering: Offering?): Builder = apply {
            this.offering = offering
        }

        /**
         * Sets whether to display a close button on the paywall screen. Only available for original template paywalls.
         * Ignored for v2 Paywalls. Defaults to true.
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

        public fun setCustomPurchaseLogic(purchaseLogic: PurchaseLogic?): Builder = apply {
            this.purchaseLogic = purchaseLogic
        }

        public fun build(): PaywallDialogOptions {
            return PaywallDialogOptions(this)
        }
    }
}
