package com.revenuecat.purchases.ui.revenuecatui.views

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.AbstractComposeView
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.OfferingSelection
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider

/**
 * View that wraps the [Paywall] Composable to display the Paywall through XML layouts and the View system.
 */
class PaywallView : AbstractComposeView {

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
        this.initialFontProvider = fontProvider
        init(context, null)
    }

    private val paywallOptionsState = mutableStateOf(
        PaywallOptions.Builder {
            dismissHandler?.invoke()
        }.build(),
    )
    private var initialOfferingId: String? = null
    private var initialFontProvider: FontProvider? = null
    private var dismissHandler: (() -> Unit)? = null
    private var listener: PaywallListener? = null
    private var shouldDisplayDismissButton: Boolean? = null
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
        val offeringSelection = if (offeringId == null) {
            OfferingSelection.None
        } else {
            OfferingSelection.OfferingId(offeringId)
        }
        paywallOptions = paywallOptions.copy(offeringSelection = offeringSelection)
    }

    /**
     * Sets the font provider to be used for the Paywall. If not set, the default one will be used.
     */
    fun setFontProvider(fontProvider: FontProvider?) {
        paywallOptions = paywallOptions.copy(fontProvider = fontProvider)
    }

    /**
     * Sets the visibility of the dismiss button in the Paywall.
     */
    fun setDisplayDismissButton(shouldDisplayDismissButton: Boolean) {
        paywallOptions = paywallOptions.copy(shouldDisplayDismissButton = shouldDisplayDismissButton)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        parseAttributes(context, attrs)
        paywallOptions = PaywallOptions.Builder { dismissHandler?.invoke() }
            .setListener(internalListener)
            .setFontProvider(initialFontProvider)
            .setOfferingId(initialOfferingId)
            .setShouldDisplayDismissButton(shouldDisplayDismissButton ?: false)
            .build()
    }

    @SuppressWarnings("DestructuringDeclarationWithTooManyEntries")
    private fun parseAttributes(context: Context, attrs: AttributeSet?) {
        val (offeringId, fontProvider, shouldDisplayDismissButton, _) =
            PaywallViewAttributesReader.parseAttributes(context, attrs, R.styleable.PaywallView) ?: return
        setOfferingId(offeringId)
        this.initialFontProvider = fontProvider
        this.shouldDisplayDismissButton = shouldDisplayDismissButton
    }

    @Composable
    override fun Content() {
        val paywallOptions by remember {
            paywallOptionsState
        }
        Paywall(paywallOptions)
    }
}
