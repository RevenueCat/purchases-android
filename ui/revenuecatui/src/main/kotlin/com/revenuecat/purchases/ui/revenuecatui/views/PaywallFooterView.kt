package com.revenuecat.purchases.ui.revenuecatui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
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
import com.revenuecat.purchases.ui.revenuecatui.fonts.CustomFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * View that wraps the [PaywallFooter] Composable to display the Paywall Footer through XML layouts and the View system.
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
class PaywallFooterView : FrameLayout {

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
        dismissHandler: (() -> Unit)? = null,
    ) : super(context) {
        setPaywallListener(listener)
        setDismissHandler(dismissHandler)
        setOfferingId(offering?.identifier)
        this.fontProvider = fontProvider
        init(context, null)
    }

    private var offeringId: String? = null
    private var fontProvider: FontProvider? = null
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
                    )
                }
            },
        )
    }

    private fun parseAttributes(context: Context, attrs: AttributeSet?) {
        var fontFamilyId: Int? = null
        var offeringIdentifier: String? = null
        context.obtainStyledAttributes(
            attrs,
            R.styleable.PaywallFooterView,
            0,
            0,
        ).apply {
            try {
                fontFamilyId = getResourceId(R.styleable.PaywallFooterView_android_fontFamily, 0)
                offeringIdentifier = getString(R.styleable.PaywallFooterView_offeringIdentifier)
            } finally {
                recycle()
            }
        }
        fontFamilyId?.let {
            val typeface = ResourcesCompat.getFont(context, it)
            if (typeface == null) {
                Logger.e("Font given for PaywallFooterView not found")
            } else {
                fontProvider = CustomFontProvider(FontFamily(typeface))
            }
        }
        offeringIdentifier?.let { setOfferingId(offeringIdentifier) }
    }
}
