package com.revenuecat.purchases.ui.revenuecatui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.OfferingSelection
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider

/**
 * View that wraps the [Paywall] Composable to display the Paywall through XML layouts and the View system.
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
class PaywallView : FrameLayout {

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    /**
     * Constructor when creating the view programmatically.
     */
    @JvmOverloads
    constructor(
        context: Context,
        offering: Offering? = null,
        listener: PaywallListener? = null,
        fontProvider: FontProvider? = null,
        shouldDisplayDismissButton: Boolean? = null,
        dismissHandler: (() -> Unit)? = null,
    ) : super(context) {
        setPaywallListener(listener)
        setDismissHandler(dismissHandler)
        setOfferingId(offering?.identifier)
        this.shouldDisplayDismissButton = shouldDisplayDismissButton
        this.fontProvider = fontProvider
        init(context, null)
    }

    private var updateOfferingId: () -> Unit = {}
    private var offeringId: String? = null
    private var fontProvider: FontProvider? = null
    private var dismissHandler: (() -> Unit)? = null
    private var listener: PaywallListener? = null
    private var shouldDisplayDismissButton: Boolean? = null
    private var internalListener: PaywallListener = object : PaywallListener {
        override fun onPurchaseStarted(rcPackage: Package) { listener?.onPurchaseStarted(rcPackage) }
        override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
            listener?.onPurchaseCompleted(customerInfo, storeTransaction)
        }
        override fun onPurchaseError(error: PurchasesError) { listener?.onPurchaseError(error) }
        override fun onRestoreStarted() { listener?.onRestoreStarted() }
        override fun onRestoreCompleted(customerInfo: CustomerInfo) { listener?.onRestoreCompleted(customerInfo) }
        override fun onRestoreError(error: PurchasesError) { listener?.onRestoreError(error) }
    }

    /**
     * Sets a [PaywallListener] to receive callbacks from the Paywall.
     */
    fun setPaywallListener(listener: PaywallListener?) {
        this.listener = listener
    }

    /**
     * Sets a dismiss handler which will be called:
     * - When the user successfully purchases
     * - If there is an error loading the offerings and the user clicks through the error dialog
     * - If the user taps on the close button
     * - If the user calls the back button with the paywall present.
     */
    fun setDismissHandler(dismissHandler: (() -> Unit)?) {
        this.dismissHandler = dismissHandler
    }

    /**
     * Sets the offering id to be used to display the Paywall. If not set, the default one will be used.
     */
    fun setOfferingId(offeringId: String?) {
        this.offeringId = offeringId
        this.updateOfferingId()
        invalidate()
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        parseAttributes(context, attrs)
        addView(
            ComposeView(context).apply {
                setContent {
                    var paywallOptions by remember {
                        mutableStateOf(PaywallOptions.Builder { dismissHandler?.invoke() }
                            .setListener(internalListener)
                            .setFontProvider(fontProvider)
                            .setOfferingId(offeringId)
                            .setShouldDisplayDismissButton(shouldDisplayDismissButton ?: false)
                            .build())
                    }
                    updateOfferingId = {
                        val newOfferingId = offeringId
                        if (newOfferingId != null) {
                            paywallOptions = paywallOptions.copy(
                                offeringSelection = OfferingSelection.OfferingId(newOfferingId)
                            )
                        }
                    }
                    Paywall(
                        options = paywallOptions,
                    )
                }
            },
        )
    }

    @SuppressWarnings("DestructuringDeclarationWithTooManyEntries")
    private fun parseAttributes(context: Context, attrs: AttributeSet?) {
        val (offeringId, fontProvider, shouldDisplayDismissButton, _) =
            PaywallViewAttributesReader.parseAttributes(context, attrs, R.styleable.PaywallView) ?: return
        setOfferingId(offeringId)
        this.fontProvider = fontProvider
        this.shouldDisplayDismissButton = shouldDisplayDismissButton
    }
}
