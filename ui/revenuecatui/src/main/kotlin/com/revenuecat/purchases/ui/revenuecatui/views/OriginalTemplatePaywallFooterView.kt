package com.revenuecat.purchases.ui.revenuecatui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.OfferingSelection
import com.revenuecat.purchases.ui.revenuecatui.OriginalTemplatePaywallFooter
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider

/**
 * View that wraps the [OriginalTemplatePaywallFooter] Composable to display the Paywall Footer
 * through XML layouts and the View system.
 */
@Deprecated(
    "Use OriginalTemplatePaywallFooterView instead",
    ReplaceWith(
        "OriginalTemplatePaywallFooterView",
    ),
)
open class PaywallFooterView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : OriginalTemplatePaywallFooterView(context, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    @JvmOverloads
    constructor(
        context: Context,
        offering: Offering? = null,
        listener: PaywallListener? = null,
        fontProvider: FontProvider? = null,
        condensed: Boolean = PaywallViewAttributesReader.DEFAULT_CONDENSED,
        dismissHandler: (() -> Unit)? = null,
    ) : this(context, attrs = null)
}

/**
 * View that wraps the [OriginalTemplatePaywallFooter] Composable to display the Paywall Footer
 * through XML layouts and the View system.
 */
open class OriginalTemplatePaywallFooterView : FrameLayout {

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
        this.initialFontProvider = fontProvider
        this.initialCondensed = condensed
        init(context, null)
    }

    private val paywallOptionsState = mutableStateOf(
        PaywallOptions.Builder {
            dismissHandler?.invoke()
        }.build(),
    )
    private var initialOfferingId: String? = null
    private var initialFontProvider: FontProvider? = null
    private var initialCondensed: Boolean = PaywallViewAttributesReader.DEFAULT_CONDENSED
    private var dismissHandler: (() -> Unit)? = null
    private var listener: PaywallListener? = null
    private var internalListener: PaywallListener = object : PaywallListener {
        override fun onPurchaseStarted(rcPackage: Package) { listener?.onPurchaseStarted(rcPackage) }
        override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
            listener?.onPurchaseCompleted(customerInfo, storeTransaction)
        }
        override fun onPurchaseError(error: PurchasesError) { listener?.onPurchaseError(error) }
        override fun onPurchaseCancelled() { listener?.onPurchaseCancelled() }
        override fun onRestoreStarted() { listener?.onRestoreStarted() }
        override fun onRestoreCompleted(customerInfo: CustomerInfo) { listener?.onRestoreCompleted(customerInfo) }
        override fun onRestoreError(error: PurchasesError) { listener?.onRestoreError(error) }
    }

    private var paywallOptions: PaywallOptions
        get() = paywallOptionsState.value
        set(value) {
            paywallOptionsState.value = value
        }

    /**
     * Sets a [PaywallListener] to receive callbacks from the Paywall.
     *
     * @note The listener callbacks will **not** be called when the app is handling purchase and restore logic itself,
     * ie when Purchases has been configured with purchasesAreCompletedBy as PurchasesAreCompletedBy.MY_APP.
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
        val offeringSelection = if (offeringId == null) {
            OfferingSelection.None
        } else {
            OfferingSelection.OfferingId(offeringId)
        }
        paywallOptions = paywallOptions.copy(offeringSelection = offeringSelection)
    }

    /**
     * Sets the font provider to be used for the Paywall. If not set, the default one will be used.
     * Only available for original template paywalls. Ignored for V2 Paywalls.
     */
    fun setFontProvider(fontProvider: FontProvider?) {
        paywallOptions = paywallOptions.copy(fontProvider = fontProvider)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        parseAttributes(context, attrs)
        paywallOptions = PaywallOptions.Builder { dismissHandler?.invoke() }
            .setListener(internalListener)
            .setFontProvider(initialFontProvider)
            .setOfferingId(initialOfferingId)
            .build()
        addView(
            ComposeView(context).apply {
                setContent {
                    val paywallOptions by remember {
                        paywallOptionsState
                    }
                    OriginalTemplatePaywallFooter(
                        options = paywallOptions,
                        condensed = initialCondensed,
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
        this.initialFontProvider = fontProvider
        condensed?.let { this.initialCondensed = it }
    }
}
