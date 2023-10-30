package com.revenuecat.purchases.ui.revenuecatui.data

import android.app.Activity
import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.extensions.getActivity
import com.revenuecat.purchases.ui.revenuecatui.helpers.ApplicationContext
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.toPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatedPaywall
import com.revenuecat.purchases.ui.revenuecatui.strings.PaywallValidationErrorStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface PaywallViewModel {
    val state: StateFlow<PaywallState>
    val actionInProgress: State<Boolean>
    val actionError: State<PurchasesError?>

    fun refreshStateIfLocaleChanged()
    fun refreshStateIfColorsChanged(colorScheme: ColorScheme)
    fun selectPackage(packageToSelect: TemplateConfiguration.PackageInfo)
    fun closeButtonPressed()

    /**
     * Purchase the selected package
     * Note: This method requires the context to be an activity or to allow reaching an activity
     */
    fun purchaseSelectedPackage(context: Context)

    fun restorePurchases()

    fun clearActionError()
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Suppress("TooManyFunctions")
internal class PaywallViewModelImpl(
    private val applicationContext: ApplicationContext,
    private val purchases: PurchasesType = PurchasesImpl(),
    private val options: PaywallOptions,
    colorScheme: ColorScheme,
    preview: Boolean = false,
) : ViewModel(), PaywallViewModel {
    private val variableDataProvider = VariableDataProvider(applicationContext, preview)

    override val state: StateFlow<PaywallState>
        get() = _state.asStateFlow()
    override val actionInProgress: State<Boolean>
        get() = _actionInProgress
    override val actionError: State<PurchasesError?>
        get() = _actionError

    private val _state: MutableStateFlow<PaywallState> = MutableStateFlow(PaywallState.Loading)
    private val _actionInProgress: MutableState<Boolean> = mutableStateOf(false)
    private val _actionError: MutableState<PurchasesError?> = mutableStateOf(null)
    private val _lastLocaleList = MutableStateFlow(getCurrentLocaleList())
    private val _colorScheme = MutableStateFlow(colorScheme)

    private val listener: PaywallListener?
        get() = options.listener

    private val mode: PaywallMode
        get() = options.mode

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
            is PaywallState.Loaded -> {
                currentState.selectPackage(packageToSelect)
            }

            else -> {
                Logger.e("Unexpected state trying to select package: $currentState")
            }
        }
    }

    override fun closeButtonPressed() {
        Logger.d("Paywalls: Close button pressed.")
        options.dismissRequest()
    }

    override fun purchaseSelectedPackage(context: Context) {
        val activity = context.getActivity() ?: error("Activity not found")
        when (val currentState = _state.value) {
            is PaywallState.Loaded -> {
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
        if (verifyNoActionInProgressOrStartAction()) { return }

        viewModelScope.launch {
            try {
                listener?.onRestoreStarted()
                val customerInfo = purchases.awaitRestore()
                Logger.i("Restore purchases successful: $customerInfo")
                listener?.onRestoreCompleted(customerInfo)
            } catch (e: PurchasesException) {
                Logger.e("Error restoring purchases: $e")
                listener?.onRestoreError(e.error)
                _actionError.value = e.error
            }

            finishAction()
        }
    }

    override fun clearActionError() {
        _actionError.value = null
    }

    private fun purchasePackage(activity: Activity, packageToPurchase: Package) {
        if (verifyNoActionInProgressOrStartAction()) { return }

        viewModelScope.launch {
            try {
                listener?.onPurchaseStarted(packageToPurchase)
                val purchaseResult = purchases.awaitPurchase(
                    PurchaseParams.Builder(activity, packageToPurchase),
                )
                listener?.onPurchaseCompleted(purchaseResult.customerInfo, purchaseResult.storeTransaction)
                options.dismissRequest()
            } catch (e: PurchasesException) {
                listener?.onPurchaseError(e.error)
                _actionError.value = e.error
            }

            finishAction()
        }
    }

    private fun updateState() {
        viewModelScope.launch {
            try {
                var currentOffering = options.offeringSelection.offering
                if (currentOffering == null) {
                    val offerings = purchases.awaitOfferings()
                    currentOffering = options.offeringSelection.offeringIdentifier?.let { offerings[it] }
                        ?: offerings.current
                }

                if (currentOffering == null) {
                    _state.value = PaywallState.Error("No offering or current offering")
                } else {
                    _state.value = calculateState(
                        currentOffering,
                        purchases.awaitCustomerInfo(),
                        _colorScheme.value,
                    )
                }
            } catch (e: PurchasesException) {
                _state.value = PaywallState.Error(e.toString())
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
    ): PaywallState {
        if (offering.availablePackages.isEmpty()) {
            return PaywallState.Error("No packages available")
        }
        val (displayablePaywall, template, error) = offering.validatedPaywall(
            colorScheme,
            applicationContext,
        )

        error?.let { validationError ->
            Logger.w(validationError.associatedErrorString(offering))
            Logger.w(PaywallValidationErrorStrings.DISPLAYING_DEFAULT)
        }

        return offering.toPaywallState(
            variableDataProvider = variableDataProvider,
            activelySubscribedProductIdentifiers = customerInfo.activeSubscriptions,
            nonSubscriptionProductIdentifiers = customerInfo.nonSubscriptionTransactions
                .map { it.productIdentifier }
                .toSet(),
            mode = mode,
            validatedPaywallData = displayablePaywall,
            template = template,
            shouldDisplayDismissButton = options.shouldDisplayDismissButton,
        )
    }

    /**
     * @return true if there already was an action in progress
     */
    private fun verifyNoActionInProgressOrStartAction(): Boolean {
        if (_actionInProgress.value) {
            Logger.d("Ignoring purchase or restore because there already is an action in progress")
            return true
        }

        _actionInProgress.value = true
        return false
    }

    private fun finishAction() {
        _actionInProgress.value = false
    }
}
