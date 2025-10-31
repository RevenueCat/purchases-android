package com.revenuecat.purchases.ui.revenuecatui.data

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.ui.revenuecatui.OfferingSelection
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallValidationResult
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.createLocaleFromString
import com.revenuecat.purchases.ui.revenuecatui.helpers.fallbackPaywall
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.toLegacyPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatedPaywall
import com.revenuecat.purchases.ui.revenuecatui.isFullScreen
import com.revenuecat.purchases.ui.revenuecatui.strings.PaywallValidationErrorStrings
import com.revenuecat.purchases.ui.revenuecatui.utils.appendQueryParameter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.MalformedURLException
import java.net.URL
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

@Suppress("TooManyFunctions")
@Stable
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

    fun getWebCheckoutUrl(launchWebCheckout: PaywallAction.External.LaunchWebCheckout): String?
    fun invalidateCustomerInfoCache()

    /**
     * Purchase the selected package
     * Note: This method requires the context to be an activity or to allow reaching an activity
     */
    fun purchaseSelectedPackage(activity: Activity?)
    suspend fun handlePackagePurchase(activity: Activity, pkg: Package?)

    fun restorePurchases()
    suspend fun handleRestorePurchases()

    fun clearActionError()
}

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
        val needsUpdateState = this.options.hashCode() != options.hashCode()
        // Some properties not considered for equality (hashCode) may have changed
        // (e.g. the listener may change in some re-renderers)
        this.options = options
        if (needsUpdateState) {
            updateState()
        }
    }

    override fun refreshStateIfLocaleChanged() {
        val currentLocaleList = getCurrentLocaleList()
        if (_lastLocaleList.value != currentLocaleList) {
            _lastLocaleList.value = currentLocaleList

            // If we have a Components paywall state, update its locale instead of recreating the entire state
            val currentState = _state.value
            if (currentState is PaywallState.Loaded.Components) {
                currentState.update(localeList = currentLocaleList.toFrameworkLocaleList())
            } else {
                updateState()
            }
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

    @Suppress("ReturnCount")
    override fun getWebCheckoutUrl(launchWebCheckout: PaywallAction.External.LaunchWebCheckout): String? {
        val customUrl = launchWebCheckout.customUrl
        val state = state.value as? PaywallState.Loaded.Components
        if (state == null) {
            Logger.e("Web checkout URL can only be constructed for loaded Components paywalls")
            return null
        }
        val behavior = launchWebCheckout.packageParamBehavior
        val (packageToUse, packageParam) = when (behavior) {
            is PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.Append ->
                (behavior.rcPackage ?: state.selectedPackageInfo?.rcPackage) to behavior.packageParam
            is PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.DoNotAppend ->
                null to null
        }
        if (customUrl != null) {
            val url = try {
                URL(customUrl)
            } catch (e: MalformedURLException) {
                Logger.e("Invalid custom URL: $customUrl", e)
                return null
            }
            val finalUrl = if (packageParam != null && packageToUse != null) {
                url.appendQueryParameter(packageParam, packageToUse.identifier)
            } else {
                url
            }
            return finalUrl.toString()
        }
        return packageToUse?.webCheckoutURL?.toString() ?: state.offering.webCheckoutURL.toString()
    }

    override fun invalidateCustomerInfoCache() {
        purchases.invalidateVirtualCurrenciesCache()
    }

    override fun purchaseSelectedPackage(activity: Activity?) {
        if (activity == null) {
            Logger.e("Activity is null, not initiating package purchase")
            return
        }
        viewModelScope.launch {
            handlePackagePurchase(activity, pkg = null)
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

    override suspend fun handlePackagePurchase(activity: Activity, pkg: Package?) {
        if (verifyNoActionInProgressOrStartAction()) {
            return
        }
        when (val currentState = _state.value) {
            is PaywallState.Loaded.Legacy -> {
                val selectedPackage = currentState.selectedPackage.value
                performPurchase(activity, selectedPackage.rcPackage)
            }
            is PaywallState.Loaded.Components -> {
                // Purchase the provided package if not null, otherwise purchase the selected package.
                val selectedPackageInfo = pkg?.let {
                    PaywallState.Loaded.Components.SelectedPackageInfo(
                        rcPackage = it,
                    )
                } ?: currentState.selectedPackageInfo
                performPurchaseIfNecessary(activity, selectedPackageInfo)
            }
            is PaywallState.Error,
            is PaywallState.Loading,
            -> Logger.e("Unexpected state trying to purchase package: $currentState")
        }
        finishAction()
    }

    private suspend fun performPurchaseIfNecessary(
        activity: Activity,
        packageInfo: PaywallState.Loaded.Components.SelectedPackageInfo?,
    ) {
        if (packageInfo == null) {
            Logger.w("Ignoring purchase request as no package is selected")
        } else {
            performPurchase(activity, packageInfo.rcPackage)
        }
    }

    @Suppress("LongMethod", "NestedBlockDepth")
    private suspend fun performPurchase(activity: Activity, packageToPurchase: Package) {
        // Call onPurchasePackageInitiated and wait for resume() to be called
        suspendCancellableCoroutine { continuation ->
            listener?.onPurchasePackageInitiated(packageToPurchase.identifier) {
                continuation.resume(Unit)
            } ?: continuation.resume(Unit)
        }

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
                val currentOffering: Offering? = when (val offeringSelection = options.offeringSelection) {
                    is OfferingSelection.OfferingType -> offeringSelection.offeringType
                    is OfferingSelection.IdAndPresentedOfferingContext -> {
                        val offerings = purchases.awaitOfferings()
                        val presentedOfferingContext = offeringSelection.presentedOfferingContext
                        val offering = offerings[offeringSelection.offeringId] ?: offerings.current
                        presentedOfferingContext?.let {
                            offering?.copy(presentedOfferingContext)
                        } ?: offering
                    }
                    is OfferingSelection.None -> {
                        val offerings = purchases.awaitOfferings()
                        offerings.current
                    }
                }

                if (currentOffering == null) {
                    _state.value = PaywallState.Error(
                        "The RevenueCat dashboard does not have a current offering configured.",
                    )
                } else {
                    _state.value = calculateState(
                        currentOffering,
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
        val preferredLocale = purchases.preferredUILocaleOverride ?: return LocaleListCompat.getDefault()

        return try {
            val locale = createLocaleFromString(preferredLocale)
            val localeList = LocaleListCompat.create(locale)
            localeList
        } catch (e: IllegalArgumentException) {
            Logger.e("Invalid preferred locale format: $preferredLocale. Using system default.", e)
            LocaleListCompat.getDefault()
        }
    }

    @Suppress("SpreadOperator")
    private fun LocaleListCompat.toFrameworkLocaleList(): android.os.LocaleList {
        val locales = Array(size()) { i -> get(i)!! }
        return android.os.LocaleList(*locales)
    }

    private fun calculateState(
        offering: Offering,
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
                Logger.e(error.associatedErrorString(offering))
            }
            Logger.e(PaywallValidationErrorStrings.DISPLAYING_DEFAULT)
        }

        return when (validationResult) {
            is PaywallValidationResult.Legacy -> offering.toLegacyPaywallState(
                variableDataProvider = variableDataProvider,
                mode = mode,
                validatedPaywallData = validationResult.displayablePaywall,
                template = validationResult.template,
                shouldDisplayDismissButton = options.shouldDisplayDismissButton,
                storefrontCountryCode = storefrontCountryCode,
            )
            is PaywallValidationResult.Components -> offering.toComponentsPaywallState(
                validationResult = validationResult,
                storefrontCountryCode = storefrontCountryCode,
                dateProvider = { Date() },
                purchases = purchases,
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
