package com.revenuecat.purchases.ui.revenuecatui.data

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallValidationResult
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.fallbackPaywall
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.toLegacyPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatedPaywall
import com.revenuecat.purchases.ui.revenuecatui.isFullScreen
import com.revenuecat.purchases.ui.revenuecatui.strings.PaywallValidationErrorStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import java.util.UUID

internal interface PaywallViewModel {
    val state: StateFlow<PaywallState>
    val resourceProvider: ResourceProvider
    val actionInProgress: State<Boolean>
    val actionError: State<PurchasesError?>

    fun refreshStateIfLocaleChanged()
    fun refreshStateIfColorsChanged(colorScheme: ColorScheme, isDark: Boolean)
    fun selectPackage(packageToSelect: TemplateConfiguration.PackageInfo)
    fun trackPaywallImpressionIfNeeded()
    fun closePaywall()

    /**
     * Purchase the selected package
     * Note: This method requires the context to be an activity or to allow reaching an activity
     */
    fun purchaseSelectedPackage(activity: Activity?)
    suspend fun handlePackagePurchase(activity: Activity)

    fun restorePurchases()
    suspend fun handleRestorePurchases()

    fun clearActionError()
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("TooManyFunctions", "LongParameterList")
internal class PaywallViewModelImpl(
    override val resourceProvider: ResourceProvider,
    private val purchases: PurchasesType = PurchasesImpl(),
    private var options: PaywallOptions,
    colorScheme: ColorScheme,
    private var isDarkMode: Boolean,
    private val shouldDisplayBlock: ((CustomerInfo) -> Boolean)?,
    preview: Boolean = false,
) : ViewModel(), PaywallViewModel {
    private val variableDataProvider = VariableDataProvider(resourceProvider, preview)

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

    private val purchaseLogic: PurchaseLogic?
        get() = options.purchaseLogic

    private var paywallPresentationData: PaywallEvent.Data? = null

    init {
        updateState()
        validateState()
    }

    fun updateOptions(options: PaywallOptions) {
        if (this.options != options) {
            this.options = options
            updateState()
        }
    }

    override fun refreshStateIfLocaleChanged() {
        if (_lastLocaleList.value != getCurrentLocaleList()) {
            _lastLocaleList.value = getCurrentLocaleList()
            updateState()
        }
    }

    override fun refreshStateIfColorsChanged(colorScheme: ColorScheme, isDark: Boolean) {
        if (isDarkMode != isDark) {
            // This is only used for events so no need to update the state here currently.
            isDarkMode = isDark
        }
        if (_colorScheme.value != colorScheme) {
            _colorScheme.value = colorScheme
            updateState()
        }
    }

    override fun selectPackage(packageToSelect: TemplateConfiguration.PackageInfo) {
        when (val currentState = _state.value) {
            is PaywallState.Loaded.Legacy -> {
                currentState.selectPackage(packageToSelect)
            }

            else -> {
                Logger.e("Unexpected state trying to select package: $currentState")
            }
        }
    }

    override fun closePaywall() {
        Logger.d("Paywalls: Close paywall initiated")
        trackPaywallClose()
        options.dismissRequest()
    }

    override fun purchaseSelectedPackage(activity: Activity?) {
        if (activity == null) {
            Logger.e("Activity is null, not initiating package purchase")
            return
        }
        viewModelScope.launch {
            handlePackagePurchase(activity)
        }
    }

    override fun restorePurchases() {
        viewModelScope.launch {
            handleRestorePurchases()
        }
    }

    override fun clearActionError() {
        _actionError.value = null
    }

    override fun trackPaywallImpressionIfNeeded() {
        if (paywallPresentationData == null) {
            paywallPresentationData = createEventData()
            track(PaywallEventType.IMPRESSION)
        }
    }

    @Suppress("NestedBlockDepth", "CyclomaticComplexMethod", "LongMethod")
    override suspend fun handleRestorePurchases() {
        if (verifyNoActionInProgressOrStartAction()) {
            return
        }
        try {
            val customRestoreHandler: (suspend (CustomerInfo) -> PurchaseLogicResult)? =
                purchaseLogic?.let { it::performRestore }

            when (purchases.purchasesAreCompletedBy) {
                PurchasesAreCompletedBy.MY_APP -> {
                    checkNotNull(customRestoreHandler) {
                        "myAppPurchaseLogic must not be null when purchases.purchasesAreCompletedBy " +
                            "is PurchasesAreCompletedBy.MY_APP"
                    }
                    val customerInfo = purchases.awaitCustomerInfo()
                    when (val result = customRestoreHandler(customerInfo)) {
                        is PurchaseLogicResult.Success -> {
                            purchases.syncPurchases()

                            shouldDisplayBlock?.let {
                                if (!it(customerInfo)) {
                                    Logger.d(
                                        "Dismissing paywall after restore since display " +
                                            "condition has not been met",
                                    )
                                    options.dismissRequest()
                                }
                            }
                        }
                        is PurchaseLogicResult.Cancellation -> {
                            // silently ignore
                        }
                        is PurchaseLogicResult.Error -> {
                            result.errorDetails?.let { _actionError.value = it }
                        }
                    }
                }

                PurchasesAreCompletedBy.REVENUECAT -> {
                    listener?.onRestoreStarted()
                    if (customRestoreHandler != null) {
                        Logger.w(
                            "myAppPurchaseLogic expected be null when " +
                                "purchases.purchasesAreCompletedBy is .REVENUECAT.\n" +
                                "myAppPurchaseLogic.performRestore will not be executed.",
                        )
                    }
                    val customerInfo = purchases.awaitRestore()
                    Logger.i("Restore purchases successful: $customerInfo")
                    listener?.onRestoreCompleted(customerInfo)

                    shouldDisplayBlock?.let {
                        if (!it(customerInfo)) {
                            Logger.d("Dismissing paywall after restore since display condition has not been met")
                            options.dismissRequest()
                        }
                    }
                }

                else -> {
                    Logger.e("Unsupported purchase completion type: ${purchases.purchasesAreCompletedBy}")
                }
            }
        } catch (e: PurchasesException) {
            Logger.e("Error restoring purchases: $e")
            listener?.onRestoreError(e.error)
            _actionError.value = e.error
        }

        finishAction()
    }

    override suspend fun handlePackagePurchase(activity: Activity) {
        if (verifyNoActionInProgressOrStartAction()) {
            return
        }
        when (val currentState = _state.value) {
            is PaywallState.Loaded.Legacy -> {
                val selectedPackage = currentState.selectedPackage.value
                if (!selectedPackage.currentlySubscribed) {
                    performPurchase(activity, selectedPackage.rcPackage)
                } else {
                    Logger.d("Ignoring purchase request for already subscribed package")
                }
            }
            is PaywallState.Loaded.Components -> {
                val selectedPackageInfo = currentState.selectedPackageInfo
                if (selectedPackageInfo == null) {
                    Logger.w("Ignoring purchase request as no package is selected")
                } else if (selectedPackageInfo.currentlySubscribed) {
                    Logger.d("Ignoring purchase request for already subscribed package")
                } else {
                    performPurchase(activity, selectedPackageInfo.rcPackage)
                }
            }
            is PaywallState.Error,
            is PaywallState.Loading,
            -> Logger.e("Unexpected state trying to purchase package: $currentState")
        }
        finishAction()
    }

    @Suppress("LongMethod", "NestedBlockDepth")
    private suspend fun performPurchase(activity: Activity, packageToPurchase: Package) {
        try {
            val customPurchaseHandler = purchaseLogic?.let { it::performPurchase }

            when (purchases.purchasesAreCompletedBy) {
                PurchasesAreCompletedBy.MY_APP -> {
                    checkNotNull(customPurchaseHandler) {
                        "myAppPurchaseLogic must not be null when purchases.purchasesAreCompletedBy " +
                            "is PurchasesAreCompletedBy.MY_APP"
                    }
                    when (val result = customPurchaseHandler.invoke(activity, packageToPurchase)) {
                        is PurchaseLogicResult.Success -> {
                            purchases.syncPurchases()
                            Logger.d("Dismissing paywall after purchase")
                            options.dismissRequest()
                        }
                        is PurchaseLogicResult.Cancellation -> {
                            trackPaywallCancel()
                        }
                        is PurchaseLogicResult.Error -> {
                            result.errorDetails?.let { _actionError.value = it }
                        }
                    }
                }
                PurchasesAreCompletedBy.REVENUECAT -> {
                    listener?.onPurchaseStarted(packageToPurchase)
                    if (customPurchaseHandler != null) {
                        Logger.e(
                            "myAppPurchaseLogic expected to be null " +
                                "when purchases.purchasesAreCompletedBy is .REVENUECAT. \n" +
                                "myAppPurchaseLogic.performPurchase will not be executed.",
                        )
                    }
                    val purchaseResult = purchases.awaitPurchase(
                        PurchaseParams.Builder(activity, packageToPurchase),
                    )
                    listener?.onPurchaseCompleted(purchaseResult.customerInfo, purchaseResult.storeTransaction)
                    Logger.d("Dismissing paywall after purchase")
                    options.dismissRequest()
                }
                else -> {
                    Logger.e("Unsupported purchase completion type: ${purchases.purchasesAreCompletedBy}")
                }
            }
        } catch (e: PurchasesException) {
            if (e.code == PurchasesErrorCode.PurchaseCancelledError) {
                trackPaywallCancel()
                listener?.onPurchaseCancelled()
            } else {
                listener?.onPurchaseError(e.error)
                _actionError.value = e.error
            }
        }

        finishAction()
    }

    private fun validateState() {
        if (purchases.purchasesAreCompletedBy == PurchasesAreCompletedBy.MY_APP && options.purchaseLogic == null) {
            _state.value = PaywallState.Error(
                "myAppPurchaseLogic is null, but is required when purchases.purchasesAreCompletedBy is " +
                    ".MY_APP. App purchases will not be successful.",
            )
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
                    _state.value = PaywallState.Error(
                        "The RevenueCat dashboard does not have a current offering configured.",
                    )
                } else {
                    _state.value = calculateState(
                        currentOffering,
                        purchases.awaitCustomerInfo(),
                        _colorScheme.value,
                        purchases.storefrontCountryCode,
                        options.mode,
                    )
                }
            } catch (e: PurchasesException) {
                _state.value = PaywallState.Error(
                    "Error ${e.code.code}: ${e.code.description}",
                )
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
        storefrontCountryCode: String?,
        mode: PaywallMode,
    ): PaywallState {
        if (offering.availablePackages.isEmpty()) {
            return PaywallState.Error("No packages available")
        }

        var validationResult = offering.validatedPaywall(colorScheme, resourceProvider)
        if (validationResult is PaywallValidationResult.Components && !mode.isFullScreen) {
            validationResult = offering.fallbackPaywall(
                colorScheme,
                resourceProvider,
                PaywallValidationError.InvalidModeForComponentsPaywall,
            )
        }

        validationResult.errors?.let { validationErrors ->
            validationErrors.forEach { error ->
                Logger.w(error.associatedErrorString(offering))
            }
            Logger.w(PaywallValidationErrorStrings.DISPLAYING_DEFAULT)
        }

        val activelySubscribedProductIds = customerInfo.activeSubscriptions
        val purchasedNonSubscriptionProductIds: Set<String> = customerInfo.nonSubscriptionTransactions
            .mapTo(mutableSetOf()) { it.productIdentifier }

        return when (validationResult) {
            is PaywallValidationResult.Legacy -> offering.toLegacyPaywallState(
                variableDataProvider = variableDataProvider,
                activelySubscribedProductIdentifiers = activelySubscribedProductIds,
                nonSubscriptionProductIdentifiers = purchasedNonSubscriptionProductIds,
                mode = mode,
                validatedPaywallData = validationResult.displayablePaywall,
                template = validationResult.template,
                shouldDisplayDismissButton = options.shouldDisplayDismissButton,
                storefrontCountryCode = storefrontCountryCode,
            )
            is PaywallValidationResult.Components -> offering.toComponentsPaywallState(
                validationResult = validationResult,
                activelySubscribedProductIds = activelySubscribedProductIds,
                purchasedNonSubscriptionProductIds = purchasedNonSubscriptionProductIds,
                storefrontCountryCode = storefrontCountryCode,
                dateProvider = { Date() },
            )
        }
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

    private fun trackPaywallClose() {
        if (paywallPresentationData != null) {
            track(PaywallEventType.CLOSE)
            paywallPresentationData = null
        }
    }

    private fun trackPaywallCancel() {
        track(PaywallEventType.CANCEL)
    }

    private fun track(eventType: PaywallEventType) {
        val eventData = paywallPresentationData
        if (eventData == null) {
            Logger.e("Paywall event data is null, not tracking event $eventType")
            return
        }
        val event = PaywallEvent(
            creationData = PaywallEvent.CreationData(UUID.randomUUID(), Date()),
            data = eventData,
            type = eventType,
        )

        purchases.track(event)
    }

    @Suppress("ReturnCount")
    private fun createEventData(): PaywallEvent.Data? =
        when (val currentState = state.value) {
            is PaywallState.Loaded.Legacy -> currentState.createEventData()

            is PaywallState.Loaded.Components -> currentState.createEventData()

            is PaywallState.Error,
            is PaywallState.Loading,
            -> {
                Logger.e("Unexpected state trying to create event data: $currentState")
                null
            }
        }

    private fun PaywallState.Loaded.Legacy.createEventData(): PaywallEvent.Data? {
        val offering = offering
        val revision = this.offering.paywall?.revision ?: this.offering.paywallComponents?.data?.revision ?: run {
            Logger.e("Null paywall revision trying to create event data")
            return null
        }
        val locale = _lastLocaleList.value.get(0) ?: Locale.getDefault()
        return PaywallEvent.Data(
            offeringIdentifier = offering.identifier,
            paywallRevision = revision,
            sessionIdentifier = UUID.randomUUID(),
            displayMode = mode.name.lowercase(),
            localeIdentifier = locale.toString(),
            darkMode = isDarkMode,
        )
    }

    private fun PaywallState.Loaded.Components.createEventData(): PaywallEvent.Data? {
        val offering = offering
        val paywallData = this.offering.paywallComponents ?: run {
            Logger.e("Null paywall revision trying to create event data")
            return null
        }
        return PaywallEvent.Data(
            offeringIdentifier = offering.identifier,
            paywallRevision = paywallData.data.revision,
            sessionIdentifier = UUID.randomUUID(),
            displayMode = mode.name.lowercase(),
            localeIdentifier = locale.toString(),
            darkMode = isDarkMode,
        )
    }
}
