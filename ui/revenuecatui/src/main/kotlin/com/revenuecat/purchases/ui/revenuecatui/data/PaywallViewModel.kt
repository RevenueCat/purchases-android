package com.revenuecat.purchases.ui.revenuecatui.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.ColorScheme
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewOptions
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.extensions.getActivity
import com.revenuecat.purchases.ui.revenuecatui.helpers.ApplicationContext
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.toPaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatedPaywall
import com.revenuecat.purchases.ui.revenuecatui.strings.PaywallValidationErrorStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URL

internal interface PaywallViewModel {
    val state: StateFlow<PaywallViewState>

    fun refreshStateIfLocaleChanged()
    fun refreshStateIfColorsChanged(colorScheme: ColorScheme)
    fun selectPackage(packageToSelect: TemplateConfiguration.PackageInfo)

    /**
     * Purchase the selected package
     * Note: This method requires the context to be an activity or to allow reaching an activity
     */
    fun purchaseSelectedPackage(context: Context)

    fun restorePurchases()

    fun openURL(url: URL, context: Context)
}

@Suppress("TooManyFunctions")
internal class PaywallViewModelImpl(
    applicationContext: ApplicationContext,
    private val options: PaywallViewOptions,
    colorScheme: ColorScheme,
    preview: Boolean = false,
) : ViewModel(), PaywallViewModel {

    private val variableDataProvider = VariableDataProvider(applicationContext, preview)

    override val state: StateFlow<PaywallViewState>
        get() = _state.asStateFlow()

    private val mode: PaywallViewMode
        get() = options.mode

    private val _state: MutableStateFlow<PaywallViewState> = MutableStateFlow(PaywallViewState.Loading)
    private val _lastLocaleList = MutableStateFlow(getCurrentLocaleList())
    private val _colorScheme = MutableStateFlow(colorScheme)

    private val listener: PaywallViewListener?
        get() = options.listener

    init {
        updateState()
    }

    override fun refreshStateIfLocaleChanged() {
        if (_lastLocaleList.value != getCurrentLocaleList()) {
            _lastLocaleList.value = getCurrentLocaleList()
            updateState()
        }
    }

    override fun refreshStateIfColorsChanged(colorScheme: ColorScheme) {
        if (_colorScheme.value != colorScheme) {
            _colorScheme.value = colorScheme
            updateState()
        }
    }

    override fun selectPackage(packageToSelect: TemplateConfiguration.PackageInfo) {
        when (val currentState = _state.value) {
            is PaywallViewState.Loaded -> {
                currentState.selectPackage(packageToSelect)
            }

            else -> {
                Logger.e("Unexpected state trying to select package: $currentState")
            }
        }
    }

    override fun purchaseSelectedPackage(context: Context) {
        val activity = context.getActivity() ?: error("Activity not found")
        when (val currentState = _state.value) {
            is PaywallViewState.Loaded -> {
                val selectedPackage = currentState.selectedPackage.value
                if (!selectedPackage.currentlySubscribed) {
                    purchasePackage(activity, selectedPackage.rcPackage)
                } else {
                    Logger.d("Ignoring purchase request for already subscribed package")
                }
            }

            else -> {
                Logger.e("Unexpected state trying to purchase package: $currentState")
            }
        }
    }

    override fun restorePurchases() {
        viewModelScope.launch {
            try {
                val customerInfo = Purchases.sharedInstance.awaitRestore()
                Logger.i("Restore purchases successful: $customerInfo")
            } catch (e: PurchasesException) {
                Logger.e("Error restoring purchases: $e")
            }
        }
    }

    override fun openURL(url: URL, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()))
        context.startActivity(intent)
    }

    private fun purchasePackage(activity: Activity, packageToPurchase: Package) {
        viewModelScope.launch {
            try {
                listener?.onPurchaseStarted(packageToPurchase)
                val purchaseResult = Purchases.sharedInstance.awaitPurchase(
                    PurchaseParams.Builder(activity, packageToPurchase).build(),
                )
                listener?.onPurchaseCompleted(purchaseResult.customerInfo, purchaseResult.storeTransaction)
            } catch (e: PurchasesException) {
                listener?.onPurchaseError(e.error)
            }
        }
    }

    private fun updateState() {
        viewModelScope.launch {
            try {
                var currentOffering = options.offeringSelection.offering
                if (currentOffering == null) {
                    val offerings = Purchases.sharedInstance.awaitOfferings()
                    currentOffering = options.offeringSelection.offeringIdentifier?.let { offerings[it] }
                        ?: offerings.current
                }

                if (currentOffering == null) {
                    _state.value = PaywallViewState.Error("No offering or current offering")
                } else {
                    _state.value = calculateState(
                        currentOffering,
                        Purchases.sharedInstance.awaitCustomerInfo(),
                        _colorScheme.value,
                    )
                }
            } catch (e: PurchasesException) {
                _state.value = PaywallViewState.Error(e.toString())
            }
        }
    }

    private fun getCurrentLocaleList(): LocaleListCompat {
        return LocaleListCompat.getDefault()
    }

    private fun calculateState(
        offering: Offering,
        customerInfo: CustomerInfo,
        colorScheme: ColorScheme,
    ): PaywallViewState {
        if (offering.availablePackages.isEmpty()) {
            return PaywallViewState.Error("No packages available")
        }
        val (displayablePaywall, template, error) = offering.validatedPaywall(colorScheme)

        error?.let { validationError ->
            Logger.w(validationError.associatedErrorString(offering))
            Logger.w(PaywallValidationErrorStrings.DISPLAYING_DEFAULT)
        }
        return offering.toPaywallViewState(
            variableDataProvider,
            customerInfo.activeSubscriptions,
            mode,
            displayablePaywall,
            template,
        )
    }
}
