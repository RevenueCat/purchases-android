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
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.paywalls.events.ExitOfferType
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.ui.revenuecatui.CustomPaywallHandler
import com.revenuecat.purchases.ui.revenuecatui.CustomPaywallHandlerParams
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
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
import java.net.URI
import java.net.URISyntaxException
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Suppress("TooManyFunctions")
@Stable
internal interface PaywallViewModel {
    val state: StateFlow<PaywallState>
    val resourceProvider: ResourceProvider
    val actionInProgress: State<Boolean>
    val actionError: State<PurchasesError?>
    val purchaseCompleted: State<Boolean>
    val preloadedExitOffering: State<Offering?>

    fun refreshStateIfLocaleChanged()
    fun refreshStateIfColorsChanged(colorScheme: ColorScheme, isDark: Boolean)
    fun selectPackage(packageToSelect: TemplateConfiguration.PackageInfo)
    fun trackPaywallImpressionIfNeeded()
    fun trackExitOffer(exitOfferType: ExitOfferType, exitOfferingIdentifier: String)
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
    fun preloadExitOffering()
}

@Suppress("TooManyFunctions", "LongParameterList", "LargeClass")
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
    override val purchaseCompleted: State<Boolean>
        get() = _purchaseCompleted
    override val preloadedExitOffering: State<Offering?>
        get() = _preloadedExitOffering

    private val _state: MutableStateFlow<PaywallState> = MutableStateFlow(PaywallState.Loading)
    private val _actionInProgress: MutableState<Boolean> = mutableStateOf(false)
    private val _actionError: MutableState<PurchasesError?> = mutableStateOf(null)
    private val _purchaseCompleted: MutableState<Boolean> = mutableStateOf(false)
    private val _preloadedExitOffering: MutableState<Offering?> = mutableStateOf(null)
    private val _lastLocaleList = MutableStateFlow(getCurrentLocaleList())
    private val _colorScheme = MutableStateFlow(colorScheme)

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private var customPaywallHandler: CustomPaywallHandler? = null

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private val listener: PaywallListener?
        get() = options.listener ?: customPaywallHandler?.paywallListener

    private val mode: PaywallMode
        get() = options.mode

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private val purchaseLogic: PurchaseLogic?
        get() = options.purchaseLogic ?: customPaywallHandler?.purchaseLogic

    private var paywallPresentationData: PaywallEvent.Data? = null

    init {
        updateState()
        validateState()
    }

    fun updateOptions(options: PaywallOptions) {
        val needsUpdateState = this.options.hashCode() != options.hashCode()
        // Some properties not considered for equality (hashCode) may have changed
        // (e.g. the listener may change in some re-renderers)

        // Reset custom handler if offering selection changes
        val oldOfferingSelection = this.options.offeringSelection
        val newOfferingSelection = options.offeringSelection
        @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
        if (oldOfferingSelection != newOfferingSelection) {
            customPaywallHandler = null
        }

        this.options = options
        if (needsUpdateState) {
            updateState()
        }
    }

    @OptIn(InternalRevenueCatAPI::class, ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private fun initializeCustomPaywallHandlerIfNeeded(offering: Offering) {
        // Only initialize once per offering
        if (customPaywallHandler == null) {
            purchases.customPaywallHandlerFactory?.let { factory ->
                val params = CustomPaywallHandlerParams(offering)
                customPaywallHandler = factory.createCustomPaywallHandler(params)
                customPaywallHandler?.let {
                    Logger.d("Custom paywall handler created for offering: ${offering.identifier}")
                }
            }
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
        val exitOffering = if (!_purchaseCompleted.value) {
            _preloadedExitOffering.value
        } else {
            null
        }
        if (exitOffering != null) {
            trackExitOffer(ExitOfferType.DISMISS, exitOffering.identifier)
        }
        paywallPresentationData = null
        val dismissWithExitOffering = options.dismissRequestWithExitOffering
        if (dismissWithExitOffering != null) {
            dismissWithExitOffering(exitOffering)
        } else {
            options.dismissRequest()
        }
    }

    override fun preloadExitOffering() {
        viewModelScope.launch {
            try {
                val currentState = _state.value
                val currentOffering = when (currentState) {
                    is PaywallState.Loaded.Legacy -> currentState.offering
                    is PaywallState.Loaded.Components -> currentState.offering
                    else -> null
                }

                val exitOfferingId = currentOffering?.paywallComponents
                    ?.data?.exitOffers?.dismiss?.offeringId
                _preloadedExitOffering.value = if (exitOfferingId != null) {
                    val offerings = purchases.awaitOfferings()
                    offerings[exitOfferingId].also { exitOffering ->
                        if (exitOffering == null) {
                            Logger.e(
                                "Exit offering with ID '$exitOfferingId' not found in available offerings. " +
                                    "Exit offer will not be displayed.",
                            )
                        }
                    }
                } else {
                    null
                }
            } catch (e: PurchasesException) {
                Logger.e("Failed to preload exit offering", e)
            }
        }
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
            val uri = try {
                URI(customUrl)
            } catch (e: URISyntaxException) {
                Logger.e("Invalid custom URI: $customUrl", e)
                return null
            }
            val finalUri = if (packageParam != null && packageToUse != null) {
                uri.appendQueryParameter(packageParam, packageToUse.identifier)
            } else {
                uri
            }
            return finalUri.toString()
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

    override fun trackExitOffer(exitOfferType: ExitOfferType, exitOfferingIdentifier: String) {
        val eventData = paywallPresentationData
        if (eventData == null) {
            Logger.e("Paywall event data is null, not tracking exit offer event")
            return
        }
        val exitOfferEventData = eventData.copy(
            exitOfferType = exitOfferType,
            exitOfferingIdentifier = exitOfferingIdentifier,
        )
        val event = PaywallEvent(
            creationData = PaywallEvent.CreationData(UUID.randomUUID(), Date()),
            data = exitOfferEventData,
            type = PaywallEventType.EXIT_OFFER,
        )
        purchases.track(event)
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
                                    _purchaseCompleted.value = true
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
                            _purchaseCompleted.value = true
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

        val shouldResume = suspendCoroutine { continuation ->
            listener?.onPurchasePackageInitiated(packageToPurchase) { shouldResume ->
                continuation.resume(shouldResume)
            } ?: continuation.resume(true)
        }

        if (!shouldResume) {
            Logger.d("Purchase cancelled listener.onPurchasePackageInitiated returned false")
            return
        }

        try {
            val customPurchaseHandler = purchaseLogic?.let { it::performPurchase }

            trackPaywallPurchaseInitiated(packageToPurchase)

            when (purchases.purchasesAreCompletedBy) {
                PurchasesAreCompletedBy.MY_APP -> {
                    checkNotNull(customPurchaseHandler) {
                        "myAppPurchaseLogic must not be null when purchases.purchasesAreCompletedBy " +
                            "is PurchasesAreCompletedBy.MY_APP"
                    }
                    when (val result = customPurchaseHandler.invoke(activity, packageToPurchase)) {
                        is PurchaseLogicResult.Success -> {
                            purchases.syncPurchases()
                            _purchaseCompleted.value = true
                            Logger.d("Dismissing paywall after purchase")
                            options.dismissRequest()
                        }
                        is PurchaseLogicResult.Cancellation -> {
                            trackPaywallCancel()
                        }
                        is PurchaseLogicResult.Error -> {
                            result.errorDetails?.let {
                                trackPaywallPurchaseError(packageToPurchase, it)
                                _actionError.value = it
                            }
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
                    _purchaseCompleted.value = true
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
                trackPaywallPurchaseError(packageToPurchase, e.error)
                listener?.onPurchaseError(e.error)
                _actionError.value = e.error
            }
        }

        finishAction()
    }

    private fun validateState() {
        if (purchases.purchasesAreCompletedBy == PurchasesAreCompletedBy.MY_APP && purchaseLogic == null) {
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
                    initializeCustomPaywallHandlerIfNeeded(currentOffering)
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
                customVariables = options.customVariables,
                defaultCustomVariables = extractDefaultCustomVariables(offering),
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
        }
    }

    private fun trackPaywallPurchaseInitiated(rcPackage: Package) {
        val eventData = paywallPresentationData
        if (eventData == null) {
            Logger.e("Paywall event data is null, not tracking purchase initiated event")
            return
        }
        val product = rcPackage.product
        val productId = if (product is GoogleStoreProduct) {
            product.productId
        } else {
            product.id
        }
        val purchaseInitiatedEventData = eventData.copy(
            packageIdentifier = rcPackage.identifier,
            productIdentifier = productId,
        )
        val event = PaywallEvent(
            creationData = PaywallEvent.CreationData(UUID.randomUUID(), Date()),
            data = purchaseInitiatedEventData,
            type = PaywallEventType.PURCHASE_INITIATED,
        )
        purchases.track(event)
    }

    private fun trackPaywallPurchaseError(rcPackage: Package, error: PurchasesError) {
        val eventData = paywallPresentationData
        if (eventData == null) {
            Logger.e("Paywall event data is null, not tracking purchase error event")
            return
        }
        val product = rcPackage.product
        val productId = if (product is GoogleStoreProduct) {
            product.productId
        } else {
            product.id
        }
        val purchaseErrorEventData = eventData.copy(
            packageIdentifier = rcPackage.identifier,
            productIdentifier = productId,
            errorCode = error.code.code,
            errorMessage = error.message,
        )
        val event = PaywallEvent(
            creationData = PaywallEvent.CreationData(UUID.randomUUID(), Date()),
            data = purchaseErrorEventData,
            type = PaywallEventType.PURCHASE_ERROR,
        )
        purchases.track(event)
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
        val paywallId = this.offering.paywall?.id ?: this.offering.paywallComponents?.data?.id
        val locale = _lastLocaleList.value.get(0) ?: Locale.getDefault()
        return PaywallEvent.Data(
            paywallIdentifier = paywallId,
            presentedOfferingContext = offering.presentedOfferingContext,
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
            paywallIdentifier = paywallData.data.id,
            presentedOfferingContext = offering.presentedOfferingContext,
            paywallRevision = paywallData.data.revision,
            sessionIdentifier = UUID.randomUUID(),
            displayMode = mode.name.lowercase(),
            localeIdentifier = locale.toString(),
            darkMode = isDarkMode,
        )
    }

    /**
     * Extracts default custom variable values from the offering's UiConfig.
     */
    private fun extractDefaultCustomVariables(offering: Offering): Map<String, CustomVariableValue> =
        offering.paywallComponents?.uiConfig?.customVariables
            ?.mapValues { (_, definition) -> CustomVariableValue.from(definition.defaultValue) }
            ?: emptyMap()

    private val Offering.presentedOfferingContext: PresentedOfferingContext
        get() = availablePackages.firstOrNull()?.presentedOfferingContext ?: PresentedOfferingContext(identifier)
}
