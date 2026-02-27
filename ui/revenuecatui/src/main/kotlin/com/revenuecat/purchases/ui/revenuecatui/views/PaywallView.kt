package com.revenuecat.purchases.ui.revenuecatui.views

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableKeyValidator
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.OfferingSelection
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.utils.Resumable

/**
 * View that wraps the [Paywall] Composable to display the Paywall through XML layouts and the View system.
 */
@Suppress("TooManyFunctions")
public class PaywallView : CompatComposeView {

    public constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        init(context, attrs)
    }

    /**
     * Constructor when creating the view programmatically with a dismiss handler.
     */
    public constructor(
        context: Context,
        offering: Offering?,
        listener: PaywallListener?,
        fontProvider: FontProvider?,
        shouldDisplayDismissButton: Boolean?,
        dismissHandler: (() -> Unit)?,
    ) : this(context, offering, listener, fontProvider, shouldDisplayDismissButton, null, dismissHandler)

    /**
     * Constructor when creating the view programmatically.
     */
    @Suppress("LongParameterList")
    @JvmOverloads
    public constructor(
        context: Context,
        offering: Offering? = null,
        listener: PaywallListener? = null,
        fontProvider: FontProvider? = null,
        shouldDisplayDismissButton: Boolean? = null,
        purchaseLogic: PaywallPurchaseLogic? = null,
        dismissHandler: (() -> Unit)? = null,
    ) : super(context) {
        setPaywallListener(listener)
        setDismissHandler(dismissHandler)
        setPurchaseLogic(purchaseLogic)
        offering?.let {
            setOfferingId(
                offeringId = it.identifier,
                presentedOfferingContext = it.availablePackages.firstOrNull()?.presentedOfferingContext,
            )
        }
        this.shouldDisplayDismissButton = shouldDisplayDismissButton
        this.initialFontProvider = fontProvider
        init(context, null)
    }

    private val paywallOptionsState = mutableStateOf(
        PaywallOptions.Builder {
            dismiss()
        }.build(),
    )
    private var initialOfferingInfo: OfferingSelection.IdAndPresentedOfferingContext? = null
    private var initialFontProvider: FontProvider? = null
    private var dismissHandler: (() -> Unit)? = null
    private var listener: PaywallListener? = null
    private var purchaseLogic: PaywallPurchaseLogic? = null
    private var shouldDisplayDismissButton: Boolean? = null
    private var internalListener: PaywallListener = object : PaywallListener {
        override fun onPurchasePackageInitiated(rcPackage: Package, resume: Resumable) {
            listener?.onPurchasePackageInitiated(rcPackage, resume) ?: resume()
        }
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
    public fun setPaywallListener(listener: PaywallListener?) {
        this.listener = listener
    }

    /**
     * Sets a dismiss handler which will be called:
     * - When the user successfully purchases
     * - If there is an error loading the offerings and the user clicks through the error dialog
     * - If the user taps on the close button
     * - If the user calls the back button with the paywall present.
     */
    public fun setDismissHandler(dismissHandler: (() -> Unit)?) {
        this.dismissHandler = dismissHandler
    }

    /**
     * Sets the [PaywallPurchaseLogic] to handle purchases and restores within the Paywall.
     * This is required when `Purchases` has been configured with
     * `purchasesAreCompletedBy` as `PurchasesAreCompletedBy.MY_APP`.
     */
    public fun setPurchaseLogic(purchaseLogic: PaywallPurchaseLogic?) {
        this.purchaseLogic = purchaseLogic
        paywallOptions = paywallOptions.copy(purchaseLogic = purchaseLogic)
    }

    /**
     * Sets the offering id and presented offering context to be used to display the Paywall.
     * If not set, the default one will be used.
     */
    @JvmOverloads
    public fun setOfferingId(offeringId: String?, presentedOfferingContext: PresentedOfferingContext? = null) {
        val offeringSelection = if (offeringId == null) {
            OfferingSelection.None
        } else {
            OfferingSelection.IdAndPresentedOfferingContext(
                offeringId = offeringId,
                presentedOfferingContext = presentedOfferingContext,
            )
        }
        paywallOptions = paywallOptions.copy(offeringSelection = offeringSelection)
    }

    /**
     * Sets the font provider to be used for the Paywall. If not set, the default one will be used.
     * Only available for original template paywalls. Ignored for V2 Paywalls.
     */
    public fun setFontProvider(fontProvider: FontProvider?) {
        paywallOptions = paywallOptions.copy(fontProvider = fontProvider)
    }

    /**
     * Sets the visibility of the dismiss button in the Paywall.
     * Only available for original template paywalls. Ignored for V2 Paywalls.
     */
    public fun setDisplayDismissButton(shouldDisplayDismissButton: Boolean) {
        paywallOptions = paywallOptions.copy(shouldDisplayDismissButton = shouldDisplayDismissButton)
    }

    /**
     * Sets custom variables to be used in paywall text. These values will replace
     * `{{ custom.key }}` or `{{ $custom.key }}` placeholders in the paywall configuration.
     *
     * @param variables A map of variable names to their [CustomVariableValue] values.
     */
    public fun setCustomVariables(variables: Map<String, CustomVariableValue>) {
        val validVariables = CustomVariableKeyValidator.validateAndFilter(variables)
        paywallOptions = paywallOptions.copy(customVariables = validVariables)
    }

    override fun onBackPressed() {
        dismissHandler?.run { dismiss() } ?: super.onBackPressed()
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        parseAttributes(context, attrs)
        paywallOptions = PaywallOptions.Builder { dismiss() }
            .setListener(internalListener)
            .setFontProvider(initialFontProvider)
            .setOfferingIdAndPresentedOfferingContext(initialOfferingInfo)
            .setShouldDisplayDismissButton(shouldDisplayDismissButton ?: false)
            .setPurchaseLogic(purchaseLogic)
            .build()
    }

    @SuppressWarnings("DestructuringDeclarationWithTooManyEntries")
    private fun parseAttributes(context: Context, attrs: AttributeSet?) {
        val (offeringId, fontProvider, shouldDisplayDismissButton, _) =
            PaywallViewAttributesReader.parseAttributes(context, attrs, R.styleable.PaywallView) ?: return
        this.initialOfferingInfo = offeringId?.let {
            OfferingSelection.IdAndPresentedOfferingContext(
                offeringId = offeringId,
                // WIP: We do not support presentedOfferingContext when using the view in XML layouts.
                presentedOfferingContext = null,
            )
        }
        this.initialFontProvider = fontProvider
        this.shouldDisplayDismissButton = shouldDisplayDismissButton
    }

    private fun dismiss() {
        dismissHandler?.invoke()
    }

    @Composable
    override fun Content() {
        val paywallOptions by remember {
            paywallOptionsState
        }
        Paywall(paywallOptions)
    }
}
