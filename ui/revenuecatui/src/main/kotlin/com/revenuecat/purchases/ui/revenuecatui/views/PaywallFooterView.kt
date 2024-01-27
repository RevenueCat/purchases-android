package com.revenuecat.purchases.ui.revenuecatui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.PaywallFooter
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider

/**
 * View that wraps the [PaywallFooter] Composable to display the Paywall Footer through XML layouts and the View system.
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
open class PaywallFooterView : FrameLayout {

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
        condensed: Boolean = PaywallViewAttributesReader.DEFAULT_CONDENSED,
        dismissHandler: (() -> Unit)? = null,
    ) : super(context) {
        setPaywallListener(listener)
        setDismissHandler(dismissHandler)
        setOfferingId(offering?.identifier)
        this.fontProvider = fontProvider
        this.condensed = condensed
        init(context, null)
    }

    private var offeringId: String? = null
    private var fontProvider: FontProvider? = null
    private var condensed: Boolean = PaywallViewAttributesReader.DEFAULT_CONDENSED
    private var dismissHandler: (() -> Unit)? = null
    private var listener: PaywallListener? = null
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
     * Sets a dismiss handler which will be called when the user successfully purchases or if there is an error
     * loading the offerings and the user clicks through the error dialog.
     */
    fun setDismissHandler(dismissHandler: (() -> Unit)?) {
        this.dismissHandler = dismissHandler
    }

    /**
     * Sets the offering id to be used to display the Paywall. If not set, the default one will be used.
     */
    fun setOfferingId(offeringId: String?) {
        this.offeringId = offeringId
        invalidate()
    }

    /**
     * Sets the font provider to be used to display the Paywall Footer View.
     */
    fun setFontProvider(fontProvider: FontProvider) {
        this.fontProvider = fontProvider
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        parseAttributes(context, attrs)
        addView(
            ComposeView(context).apply {
                setContent {
                    val paywallOptions = PaywallOptions.Builder { dismissHandler?.invoke() }
                        .setListener(internalListener)
                        .setFontProvider(fontProvider)
                        .setOfferingId(offeringId)
                        .build()
                    PaywallFooter(
                        options = paywallOptions,
                        condensed = condensed,
                    )
                }
            },
        )
    }

    @SuppressWarnings("DestructuringDeclarationWithTooManyEntries")
    private fun parseAttributes(context: Context, attrs: AttributeSet?) {
        val (offeringId, fontProvider, _, condensed) =
            PaywallViewAttributesReader.parseAttributes(context, attrs, R.styleable.PaywallFooterView) ?: return
        setOfferingId(offeringId)
        this.fontProvider = fontProvider
        condensed?.let { this.condensed = it }
    }
}
