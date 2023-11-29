package com.revenuecat.purchases.ui.revenuecatui.fragment

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.PaywallFooter
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions

/**
 * WIP: Add docs
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
class PaywallFooterView : FrameLayout {

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

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
     * WIP: Add docs
     */
    fun setPaywallListener(listener: PaywallListener) {
        this.listener = listener
    }

    /**
     * WIP: Add docs
     */
    fun setDismissHandler(dismissHandler: () -> Unit) {
        this.dismissHandler = dismissHandler
    }

    @Suppress("UnusedParameter")
    private fun init(context: Context, attrs: AttributeSet?) {
//        WIP: Add font support
//        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PaywallFooterView, 0, 0)
//        val fontProvider = typedArray
//          .getString(R.styleable.PaywallFooterView_android_fontFamily)?.let { fontFamilyName ->
//            val font = Typeface.create() ResourcesCompat.getFont(context, )
//            object : FontProvider {
//                override fun getFont(type: TypographyType): FontFamily? {
//                    return FontFamily(Font(fontFamilyName))
//                }
//            }
//        }
        addView(
            ComposeView(context).apply {
                setContent {
                    PaywallFooter(
                        options = PaywallOptions.Builder { dismissHandler?.invoke() }
                            .setListener(internalListener)
                            .build(),
                    )
                }
            },
        )
    }
}
