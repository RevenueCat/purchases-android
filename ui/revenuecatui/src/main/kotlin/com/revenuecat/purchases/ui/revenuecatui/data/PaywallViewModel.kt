@file:OptIn(InternalRevenueCatAPI::class)

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
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowDataResult
import com.revenuecat.purchases.common.workflows.WorkflowScreenType
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowTriggerAction
import com.revenuecat.purchases.common.workflows.WorkflowTriggerType
import com.revenuecat.purchases.common.workflows.events.WorkflowEvent
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.paywalls.components.common.ProductChangeConfig
import com.revenuecat.purchases.paywalls.events.ExitOfferType
import com.revenuecat.purchases.paywalls.events.PaywallComponentInteractionData
import com.revenuecat.purchases.paywalls.events.PaywallComponentType
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.OfferingSelection
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogicParams
import com.revenuecat.purchases.ui.revenuecatui.ProductChange
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.extensions.calculateOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallValidationResult
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.createLocaleFromString
import com.revenuecat.purchases.ui.revenuecatui.helpers.fallbackPaywall
import com.revenuecat.purchases.ui.revenuecatui.helpers.paywallProductIdentifier
import com.revenuecat.purchases.ui.revenuecatui.helpers.resolveWebCheckoutUrlForInteraction
import com.revenuecat.purchases.ui.revenuecatui.helpers.safeResume
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.toLegacyPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatedPaywall
import com.revenuecat.purchases.ui.revenuecatui.isFullScreen
import com.revenuecat.purchases.ui.revenuecatui.strings.PaywallValidationErrorStrings
import com.revenuecat.purchases.ui.revenuecatui.workflow.NavigationDirection
import com.revenuecat.purchases.ui.revenuecatui.workflow.WorkflowNavigator
import com.revenuecat.purchases.ui.revenuecatui.workflow.WorkflowScreenMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale
import java.util.UUID

@Suppress("TooManyFunctions")
@Stable
internal interface PaywallViewModel {
    val state: StateFlow<PaywallState>
    val resourceProvider: ResourceProvider
    val actionInProgress: State<Boolean>
    val actionError: State<PurchasesError?>
    val purchaseCompleted: State<Boolean>

    fun refreshStateIfLocaleChanged()
    fun refreshStateIfColorsChanged(colorScheme: ColorScheme, isDark: Boolean)
    fun selectPackage(packageToSelect: TemplateConfiguration.PackageInfo)
    fun trackPaywallImpressionIfNeeded()
    fun trackExitOffer(exitOfferType: ExitOfferType, exitOfferingIdentifier: String)
    fun trackComponentInteraction(data: PaywallComponentInteractionData)

    fun trackComponentInteraction(
        componentType: PaywallComponentType,
        componentName: String?,
        componentValue: String,
        componentUrl: String? = null,
    ) {
        trackComponentInteraction(
            PaywallComponentInteractionData(
                componentType = componentType,
                componentName = componentName,
                componentValue = componentValue,
                componentUrl = componentUrl,
            ),
        )
    }
    fun closePaywall(result: PaywallResult? = null)

    /**
     * Workflow-related UI state. Non-null when the loaded paywall is a multi-step workflow.
     */
    val workflowState: State<WorkflowPaywallUiState?>

    fun handleWorkflowAction(componentId: String, triggerType: WorkflowTriggerType)

    /**
     * Handles back navigation within a workflow. Returns true if consumed by the workflow
     * (previous step rendered), false to fall through to dismiss.
     */
    fun handleBackNavigation(): Boolean

    /**
     * Called by the UI when a slide animation completes. Clears the [WorkflowPendingTransition]
     * for the given [transitionId] so the outgoing step can be removed from the slot table.
     * A guard on [transitionId] prevents stale callbacks from clobbering a newer transition.
     */
    fun onTransitionComplete(transitionId: Int)

    fun getWebCheckoutUrl(launchWebCheckout: PaywallAction.External.LaunchWebCheckout): String?
    fun invalidateCustomerInfoCache()

    /**
     * Purchase the selected package
     * Note: This method requires the context to be an activity or to allow reaching an activity
     */
    fun purchaseSelectedPackage(activity: Activity?)
    suspend fun handlePackagePurchase(activity: Activity, pkg: Package?, resolvedOffer: ResolvedOffer? = null)

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
    private val productChangeCalculator: ProductChangeCalculator = ProductChangeCalculator(purchases),
    private val useWorkflowsEndpoint: Boolean = purchases.useWorkflows,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
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
    override val workflowState: State<WorkflowPaywallUiState?>
        get() = _workflowState

    internal val preloadedExitOffering: Offering?
        get() = (exitOfferData as? ExitOfferData.Configured)?.preloadedOffering

    private val _state: MutableStateFlow<PaywallState> = MutableStateFlow(PaywallState.Loading)
    private val _actionInProgress: MutableState<Boolean> = mutableStateOf(false)
    private val _actionError: MutableState<PurchasesError?> = mutableStateOf(null)
    private val _purchaseCompleted: MutableState<Boolean> = mutableStateOf(false)
    private val _workflowState: MutableState<WorkflowPaywallUiState?> = mutableStateOf(null)
    private var workflowTraceId: String = UUID.randomUUID().toString()
    private val _lastLocaleList = MutableStateFlow(getCurrentLocaleList())
    private val _colorScheme = MutableStateFlow(colorScheme)

    private val listener: PaywallListener?
        get() = options.listener

    private val mode: PaywallMode
        get() = options.mode

    private val purchaseLogic: PaywallPurchaseLogic?
        get() = options.purchaseLogic

    private var paywallPresentationData: PaywallEvent.Data? = null

    private var workflowNavigator: WorkflowNavigator? = null
    private var currentWorkflowResult: WorkflowDataResult? = null
    private var currentWorkflowOfferings: Offerings? = null
    private var currentWorkflowPresentedOfferingContext: PresentedOfferingContext? = null
    private var currentWorkflowStepTracksPaywallEvents = true
    private val workflowStepStateCache = mutableMapOf<String, PaywallState.Loaded.Components>()

    // Shared across all screens of a workflow presentation so state-driven values survive screen navigation.
    private var currentWorkflowStateStore: PaywallStateStore? = null
    private var preWarmJob: Job? = null
    private var transitionIdCounter: Int = 0

    // Per-session flag: a completed purchase or a successful restore is a natural workflow exit, not an
    // abandonment, so it suppresses workflows_close. The session boundary is the dismiss (reset in
    // clearWorkflowState), NOT each render/refresh, so an in-session completion that leaves the paywall
    // visible (e.g. a restore with no shouldDisplayBlock) survives option/locale/color refreshes. Tracked
    // separately from the sticky, public _purchaseCompleted (which also gates exit offers and is observed
    // by the host, and which a restore only sets when shouldDisplayBlock auto-dismisses). Mirrors iOS's
    // per-session hasCompletedInSession (purchased || restored), reset at the dismiss boundary.
    private var workflowCompletedInSession: Boolean = false

    private enum class WorkflowStepEntryReason(val value: String) {
        START("start"),
        FORWARD("forward"),
        BACK("back"),
    }

    private sealed interface ExitOfferData {
        val preloadRequested: Boolean

        data class Loading(override val preloadRequested: Boolean = false) : ExitOfferData
        data class Unavailable(override val preloadRequested: Boolean = false) : ExitOfferData

        data class Configured(
            val offeringId: String,
            val offerings: Offerings,
            val triggeringWorkflowStepId: String? = null,
            override val preloadRequested: Boolean = false,
            val preloadedOffering: Offering? = null,
        ) : ExitOfferData

        fun withPreloadRequested(): ExitOfferData = when (this) {
            is Loading -> copy(preloadRequested = true)
            is Unavailable -> copy(preloadRequested = true)
            is Configured -> copy(preloadRequested = true)
        }
    }

    private var exitOfferData: ExitOfferData = ExitOfferData.Loading()
    private var updateStateJob: Job? = null

    private data class PaywallPresentationFingerprint(
        val paywallIdentifier: String?,
        val presentedOfferingContext: PresentedOfferingContext,
        val paywallRevision: Int,
        val displayMode: String,
        val localeIdentifier: String,
        val darkMode: Boolean,
    )

    private data class ResolvedOfferingSelection(
        val selectedOffering: Offering?,
        val offeringsForExitOfferLookup: Offerings?,
    )

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
                val localeFrameworkList = currentLocaleList.toFrameworkLocaleList()
                currentState.update(localeList = localeFrameworkList)
                workflowStepStateCache.values.forEach { it.update(localeList = localeFrameworkList) }
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
            if (workflowNavigator != null) {
                // Rebuild step states in-place to avoid resetting the user's navigation position.
                rebuildWorkflowStepStates()
            } else {
                updateState()
            }
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

    override fun closePaywall(result: PaywallResult?) {
        Logger.d("Paywalls: Close paywall initiated")
        trackCurrentWorkflowStepCompleted()
        if (!workflowCompletedInSession) {
            trackCurrentWorkflowAbandoned()
        }
        trackPaywallClose()
        val exitOffering = if (!_purchaseCompleted.value && shouldTriggerExitOfferForCurrentStep) {
            preloadedExitOffering
        } else {
            null
        }
        if (exitOffering != null) {
            trackExitOffer(ExitOfferType.DISMISS, exitOffering.identifier)
        }
        paywallPresentationData = null
        clearWorkflowState()
        val dismissWithExitOffering = options.dismissRequestWithExitOffering
        if (dismissWithExitOffering != null) {
            dismissWithExitOffering(exitOffering, result)
        } else {
            options.dismissRequest()
        }
    }

    override fun preloadExitOffering() {
        exitOfferData = exitOfferData.withPreloadRequested().resolveIfNeeded()
    }

    private fun cancelStateUpdate() {
        updateStateJob?.cancel()
        // Exit offer data is intentionally preserved here: locale/color/options refreshes should
        // not discard resolution that already ran. The async update sets new data via updateExitOfferData.
    }

    private fun clearWorkflowState() {
        preWarmJob?.cancel()
        preWarmJob = null
        workflowNavigator = null
        currentWorkflowResult = null
        currentWorkflowOfferings = null
        currentWorkflowPresentedOfferingContext = null
        currentWorkflowStepTracksPaywallEvents = true
        workflowStepStateCache.clear()
        currentWorkflowStateStore = null
        _workflowState.value = null
        // The dismiss is the session boundary: the next presentation on this ViewModel is a new session,
        // so completion from this one must not suppress its abandonment. Runs after closePaywall has
        // already made its workflows_close decision. A refresh/re-render of an open session does not
        // pass through here, so an in-session completion (e.g. a restore that leaves the paywall up)
        // is preserved across refreshes.
        workflowCompletedInSession = false
    }

    private fun updateExitOfferData(data: ExitOfferData) {
        val propagated = if (exitOfferData.preloadRequested) data.withPreloadRequested() else data
        exitOfferData = propagated.resolveIfNeeded()
    }

    private fun ExitOfferData.resolveIfNeeded(): ExitOfferData {
        if (this !is ExitOfferData.Configured || !preloadRequested || preloadedOffering != null) return this
        val resolved = offerings[offeringId]
        if (resolved == null) {
            Logger.w("Paywalls: Exit offering '$offeringId' not found in available offerings.")
        }
        return copy(preloadedOffering = resolved)
    }

    private val shouldTriggerExitOfferForCurrentStep: Boolean
        get() = when (val loadedExitOfferData = exitOfferData) {
            is ExitOfferData.Configured -> {
                val triggeringWorkflowStepId = loadedExitOfferData.triggeringWorkflowStepId
                triggeringWorkflowStepId == null || _workflowState.value?.currentStepId == triggeringWorkflowStepId
            }
            is ExitOfferData.Loading,
            is ExitOfferData.Unavailable,
            -> false
        }

    override fun getWebCheckoutUrl(launchWebCheckout: PaywallAction.External.LaunchWebCheckout): String? {
        val state = state.value as? PaywallState.Loaded.Components
        if (state == null) {
            Logger.e("Web checkout URL can only be constructed for loaded Components paywalls")
            return null
        }
        return state.resolveWebCheckoutUrlForInteraction(launchWebCheckout)
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

    @Suppress("ReturnCount")
    override fun trackPaywallImpressionIfNeeded() {
        val isWorkflowPresentation = currentWorkflowResult != null
        if (isWorkflowPresentation && !currentWorkflowStepTracksPaywallEvents) {
            paywallPresentationData = null
            return
        }

        val targetFingerprint = computePresentationFingerprint() ?: return
        val existing = paywallPresentationData

        if (existing?.presentationFingerprint() == targetFingerprint) {
            // Impressions are de-duped by visual presentation, but workflow attribution
            // is contextual and can change while the presentation stays the same.
            paywallPresentationData = existing.withCurrentWorkflowMetadata()
            return
        }

        if (existing != null) {
            if (!isWorkflowPresentation) {
                track(PaywallEventType.CLOSE)
            }
            paywallPresentationData = null
        }

        val newData = createEventData() ?: return
        paywallPresentationData = newData
        track(PaywallEventType.IMPRESSION)
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

    override fun trackComponentInteraction(data: PaywallComponentInteractionData) {
        val eventData = paywallPresentationData
        if (eventData == null) {
            if (currentWorkflowResult != null && !currentWorkflowStepTracksPaywallEvents) return
            Logger.e("Paywall event data is null, not tracking paywall component interaction")
            return
        }
        val event = PaywallEvent(
            creationData = PaywallEvent.CreationData(UUID.randomUUID(), Date()),
            data = eventData,
            type = PaywallEventType.COMPONENT_INTERACTION,
            componentInteraction = data,
        )
        purchases.track(event)
    }

    @Suppress("NestedBlockDepth", "CyclomaticComplexMethod", "LongMethod")
    override suspend fun handleRestorePurchases() {
        if (verifyNoActionInProgressOrStartAction()) {
            return
        }
        val shouldResume = suspendCancellableCoroutine { continuation ->
            Logger.d("Restore Purchases Initiated… waiting for listener.onRestoreInitiated to proceed.")
            listener?.onRestoreInitiated { shouldResume ->
                continuation.safeResume(shouldResume)
            } ?: continuation.safeResume(true)
        }

        val detail = if (shouldResume) "will" else "will not"
        Logger.d("Restore Purchases gate complete. The SDK **$detail** attempt to restore purchases.")

        if (!shouldResume) {
            finishAction()
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
                            val updatedCustomerInfo = purchases.awaitSyncPurchases()
                            // A successful restore is a natural workflow exit, not an abandonment, even if
                            // the paywall stays visible (no shouldDisplayBlock). Suppress workflows_close.
                            workflowCompletedInSession = true

                            shouldDisplayBlock?.let {
                                if (!it(updatedCustomerInfo)) {
                                    _purchaseCompleted.value = true
                                    Logger.d(
                                        "Dismissing paywall after restore since display " +
                                            "condition has not been met",
                                    )
                                    closePaywall(PaywallResult.Restored(updatedCustomerInfo))
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
                    // A successful restore is a natural workflow exit, not an abandonment, even if the
                    // paywall stays visible (no shouldDisplayBlock). Suppress workflows_close on dismiss.
                    workflowCompletedInSession = true
                    listener?.onRestoreCompleted(customerInfo)

                    shouldDisplayBlock?.let {
                        if (!it(customerInfo)) {
                            _purchaseCompleted.value = true
                            Logger.d("Dismissing paywall after restore since display condition has not been met")
                            trackCurrentWorkflowStepCompleted()
                            options.dismissRequest()
                            // Bypasses closePaywall, so run the same session cleanup here to reset
                            // workflowCompletedInSession at the dismiss boundary.
                            clearWorkflowState()
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

    override suspend fun handlePackagePurchase(activity: Activity, pkg: Package?, resolvedOffer: ResolvedOffer?) {
        if (verifyNoActionInProgressOrStartAction()) {
            return
        }
        when (val currentState = _state.value) {
            is PaywallState.Loaded.Legacy -> {
                val selectedPackage = currentState.selectedPackage.value
                performPurchase(
                    activity = activity,
                    packageToPurchase = selectedPackage.rcPackage,
                    subscriptionOption = null,
                )
            }
            is PaywallState.Loaded.Components -> {
                val selectedPackageInfo = pkg?.let {
                    PaywallState.Loaded.Components.SelectedPackageInfo(
                        rcPackage = it,
                        resolvedOffer = resolvedOffer,
                        uniqueId = it.identifier,
                        offerEligibility = calculateOfferEligibility(resolvedOffer, it),
                    )
                } ?: currentState.selectedPackageInfo
                val productChangeConfig = currentState.offering.paywallComponents?.data?.productChangeConfig
                performPurchaseIfNecessary(activity, selectedPackageInfo, productChangeConfig)
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
        productChangeConfig: ProductChangeConfig?,
    ) {
        if (packageInfo == null) {
            Logger.w("Ignoring purchase request as no package is selected")
        } else {
            performPurchase(
                activity = activity,
                packageToPurchase = packageInfo.rcPackage,
                productChangeConfig = productChangeConfig,
                subscriptionOption = packageInfo.resolvedOffer?.subscriptionOption,
            )
        }
    }

    @Suppress("LongMethod", "NestedBlockDepth", "CyclomaticComplexMethod")
    private suspend fun performPurchase(
        activity: Activity,
        packageToPurchase: Package,
        productChangeConfig: ProductChangeConfig? = null,
        subscriptionOption: SubscriptionOption?,
    ) {
        // Call onPurchasePackageInitiated and wait for resume() to be called

        val shouldResume = suspendCancellableCoroutine { continuation ->
            listener?.onPurchasePackageInitiated(packageToPurchase) { shouldResume ->
                continuation.safeResume(shouldResume)
            } ?: continuation.safeResume(true)
        }

        if (!shouldResume) {
            Logger.d("Purchase cancelled listener.onPurchasePackageInitiated returned false")
            return
        }

        try {
            trackPaywallPurchaseInitiated(packageToPurchase)

            val productChangeInfo = productChangeConfig?.let {
                productChangeCalculator.calculateProductChangeInfo(packageToPurchase, it)
            }

            when (purchases.purchasesAreCompletedBy) {
                PurchasesAreCompletedBy.MY_APP -> {
                    val myAppPurchaseLogic = checkNotNull(purchaseLogic) {
                        "myAppPurchaseLogic must not be null when purchases.purchasesAreCompletedBy " +
                            "is PurchasesAreCompletedBy.MY_APP"
                    }
                    val purchaseParams = PaywallPurchaseLogicParams(
                        rcPackage = packageToPurchase,
                        productChange = productChangeInfo?.let {
                            ProductChange(
                                oldProductId = it.oldProductId,
                                replacementMode = it.replacementMode,
                            )
                        },
                        subscriptionOption = subscriptionOption,
                    )
                    val result = myAppPurchaseLogic.performPurchase(activity, purchaseParams)
                    when (result) {
                        is PurchaseLogicResult.Success -> {
                            val customerInfo = purchases.awaitSyncPurchases()
                            _purchaseCompleted.value = true
                            // Set before closePaywall so the abandonment gate sees this as a completion.
                            workflowCompletedInSession = true
                            Logger.d("Dismissing paywall after purchase")
                            closePaywall(PaywallResult.Purchased(customerInfo))
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
                    if (purchaseLogic != null) {
                        Logger.e(
                            "myAppPurchaseLogic expected to be null " +
                                "when purchases.purchasesAreCompletedBy is .REVENUECAT. \n" +
                                "myAppPurchaseLogic.performPurchase will not be executed.",
                        )
                    }

                    // Use subscription option from resolved offer if available, otherwise use package
                    val purchaseParamsBuilder = if (subscriptionOption != null) {
                        PurchaseParams.Builder(activity, subscriptionOption)
                            .presentedOfferingContext(packageToPurchase.presentedOfferingContext)
                    } else {
                        PurchaseParams.Builder(activity, packageToPurchase)
                    }

                    if (productChangeInfo != null) {
                        Logger.d(
                            "Performing product change from ${productChangeInfo.oldProductId} " +
                                "with mode ${productChangeInfo.replacementMode}",
                        )
                        purchaseParamsBuilder
                            .oldProductId(productChangeInfo.oldProductId)
                            .replacementMode(productChangeInfo.replacementMode)
                    }

                    val purchaseResult = purchases.awaitPurchase(purchaseParamsBuilder)
                    _purchaseCompleted.value = true
                    workflowCompletedInSession = true
                    listener?.onPurchaseCompleted(purchaseResult.customerInfo, purchaseResult.storeTransaction)
                    Logger.d("Dismissing paywall after purchase")
                    trackCurrentWorkflowStepCompleted()
                    options.dismissRequest()
                    // This direct-dismiss completion bypasses closePaywall, so run the same session
                    // cleanup here to reset workflowCompletedInSession at the dismiss boundary.
                    clearWorkflowState()
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
        if (purchases.purchasesAreCompletedBy == PurchasesAreCompletedBy.MY_APP && options.purchaseLogic == null) {
            _state.value = PaywallState.Error(
                "myAppPurchaseLogic is null, but is required when purchases.purchasesAreCompletedBy is " +
                    ".MY_APP. App purchases will not be successful.",
            )
        }
    }
    private fun updateState() {
        cancelStateUpdate()
        updateStateJob = viewModelScope.launch {
            try {
                updateStateFromOffering(options.offeringSelection)
            } catch (e: PurchasesException) {
                updateExitOfferData(ExitOfferData.Unavailable())
                _state.value = PaywallState.Error(
                    "Error ${e.code.code}: ${e.code.description}",
                )
            }
        }
    }

    private suspend fun updateStateFromOffering(offeringSelection: OfferingSelection) {
        // Injected workflow (e.g. mobile app preview): render locally, no /workflows fetch.
        if (presentInjectedWorkflowIfNeeded(offeringSelection)) {
            return
        }

        val resolvedOfferingSelection = resolveOfferingSelection(offeringSelection)
        val selectedOffering = resolvedOfferingSelection.selectedOffering

        // When workflows are enabled, every non-legacy paywall is served through the /workflows
        // endpoint. `offering.paywall == null` is the durable marker of a non-legacy (workflow)
        // paywall: a legacy v1 paywall always carries `offering.paywall`, and that field stays
        // even after `paywallComponents` is removed and all V2 paywalls move to workflows. We
        // deliberately do NOT gate on `paywallComponents`, which is going away.
        if (useWorkflowsEndpoint && selectedOffering != null && selectedOffering.paywall == null) {
            presentWorkflow(selectedOffering, resolvedOfferingSelection.offeringsForExitOfferLookup)
            return
        }

        val exitOfferingId = selectedOffering?.paywallComponents?.data?.exitOffers?.dismiss?.offeringId

        val offerings = resolvedOfferingSelection.offeringsForExitOfferLookup
        updateExitOfferData(
            if (exitOfferingId != null && offerings != null) {
                ExitOfferData.Configured(
                    offeringId = exitOfferingId,
                    offerings = offerings,
                )
            } else {
                ExitOfferData.Unavailable()
            },
        )
        updatePaywallState(selectedOffering)
    }

    private fun presentInjectedWorkflowIfNeeded(offeringSelection: OfferingSelection): Boolean {
        val injectedWorkflow = options.injectedWorkflow ?: return false
        val offering = offeringSelection.offering
        if (offering == null) {
            Logger.w(
                "Paywalls: injectedWorkflow set without a concrete Offering (use setOffering); " +
                    "workflow screens may fail to resolve their packages.",
            )
        }
        val offerings = Offerings(
            current = offering,
            all = offering?.let { mapOf(it.identifier to it) } ?: emptyMap(),
        )
        startWorkflowPresentation(injectedWorkflow, offerings, offering?.presentedOfferingContext)
        return true
    }

    private suspend fun presentWorkflow(offering: Offering, preloadedOfferings: Offerings?) {
        // Prefer the configured workflow id, which aligns with the prefetch cache key. Fall back to
        // the offering id, which the backend lazily converts into a workflow for paywalls not yet
        // converted to a workflow.
        val workflowIdentifier = purchases.workflowIdForOfferingId(offering.identifier) ?: offering.identifier
        coroutineScope {
            val fetchResultDeferred = async { purchases.awaitGetWorkflow(workflowIdentifier) }
            val offeringsDeferred = async { preloadedOfferings ?: purchases.awaitOfferings() }
            startWorkflowPresentation(
                fetchResultDeferred.await(),
                offeringsDeferred.await(),
                offering.presentedOfferingContext,
            )
        }
    }

    private suspend fun resolveOfferingSelection(offeringSelection: OfferingSelection): ResolvedOfferingSelection =
        when (offeringSelection) {
            is OfferingSelection.OfferingType -> {
                val hasExitOffer = offeringSelection.offeringType.paywallComponents
                    ?.data?.exitOffers?.dismiss?.offeringId != null
                val offerings = if (hasExitOffer) {
                    try {
                        purchases.awaitOfferings()
                    } catch (e: PurchasesException) {
                        Logger.w("Paywalls: Failed to fetch offerings for exit offer preloading: ${e.message}")
                        null
                    }
                } else {
                    null
                }
                ResolvedOfferingSelection(
                    selectedOffering = offeringSelection.offeringType,
                    offeringsForExitOfferLookup = offerings,
                )
            }
            is OfferingSelection.IdAndPresentedOfferingContext -> {
                val offerings = purchases.awaitOfferings()
                val presentedOfferingContext = offeringSelection.presentedOfferingContext
                val offering = offerings[offeringSelection.offeringId] ?: offerings.current
                val offeringWithContext = presentedOfferingContext?.let {
                    offering?.copy(presentedOfferingContext)
                } ?: offering
                ResolvedOfferingSelection(
                    selectedOffering = offeringWithContext,
                    offeringsForExitOfferLookup = offerings,
                )
            }

            is OfferingSelection.None -> {
                val offerings = purchases.awaitOfferings()
                ResolvedOfferingSelection(
                    selectedOffering = offerings.current,
                    offeringsForExitOfferLookup = offerings,
                )
            }
        }

    private fun updatePaywallState(currentOffering: Offering?) {
        if (currentOffering == null) {
            _state.value = PaywallState.Error(
                "You do not have a current offering configured in the RevenueCat dashboard.",
            )
        } else {
            _state.value = calculateState(
                currentOffering,
                _colorScheme.value,
                purchases.storefrontCountryCode,
                options.mode,
            )
        }
    }

    internal fun startWorkflowPresentationFromResult(
        fetchResult: WorkflowDataResult,
        offerings: Offerings,
        presentedOfferingContext: PresentedOfferingContext?,
    ) {
        cancelStateUpdate()
        startWorkflowPresentation(fetchResult, offerings, presentedOfferingContext)
    }

    @Suppress("ReturnCount")
    private fun startWorkflowPresentation(
        fetchResult: WorkflowDataResult,
        offerings: Offerings,
        presentedOfferingContext: PresentedOfferingContext?,
    ) {
        val workflow = fetchResult.workflow
        val initialStep = workflow.steps[workflow.initialStepId]
        if (initialStep == null) {
            updateExitOfferData(ExitOfferData.Unavailable())
            _state.value = PaywallState.Error(
                "Initial step '${workflow.initialStepId}' not found in workflow '${workflow.id}'",
            )
            return
        }

        // Close the lifecycle of any step the user was already on before starting a new presentation.
        // Uses the old currentWorkflowResult and _workflowState before either is mutated below.
        trackCurrentWorkflowStepCompleted()

        currentWorkflowResult = fetchResult
        currentWorkflowOfferings = offerings
        currentWorkflowPresentedOfferingContext = presentedOfferingContext
        workflowNavigator = WorkflowNavigator(workflow)
        val dismissExitOffer = workflow.dismissExitOffer
        updateExitOfferData(
            dismissExitOffer?.let {
                ExitOfferData.Configured(
                    offeringId = it.offeringId,
                    offerings = offerings,
                    triggeringWorkflowStepId = it.stepId,
                )
            } ?: ExitOfferData.Unavailable(),
        )

        val stepWithPackagesId = workflow.singleStepFallbackId
        if (stepWithPackagesId != null && workflow.steps[stepWithPackagesId] == null) {
            Logger.w("Workflow singleStepFallbackId '$stepWithPackagesId' not found in steps")
        }
        buildWorkflowStates(
            workflow = workflow,
            offerings = offerings,
            presentedOfferingContext = presentedOfferingContext,
            currentStep = initialStep,
            isNewWorkflowImpression = true,
        )
    }

    /**
     * Rebuilds workflow step states with the current color scheme without resetting the
     * navigator position. Used when colors change while a workflow paywall is active,
     * so the user is not silently sent back to the first step.
     */
    @Suppress("ReturnCount")
    private fun rebuildWorkflowStepStates() {
        val result = currentWorkflowResult ?: return
        val offerings = currentWorkflowOfferings ?: return
        val currentStep = workflowNavigator?.currentStep ?: return
        buildWorkflowStates(
            workflow = result.workflow,
            offerings = offerings,
            presentedOfferingContext = currentWorkflowPresentedOfferingContext,
            currentStep = currentStep,
            isNewWorkflowImpression = false,
        )
    }

    /**
     * Clears any cached workflow step states and builds the state for [currentStep] (plus the
     * package-bearing step, if different) so the UI can render the paywall. Also kicks off
     * pre-warming of the remaining steps' caches.
     */
    private fun buildWorkflowStates(
        workflow: PublishedWorkflow,
        offerings: Offerings,
        presentedOfferingContext: PresentedOfferingContext?,
        currentStep: WorkflowStep,
        isNewWorkflowImpression: Boolean,
    ) {
        preWarmJob?.cancel()
        workflowStepStateCache.clear()
        _workflowState.value = null
        if (isNewWorkflowImpression) {
            workflowTraceId = UUID.randomUUID().toString()
            // Fresh presentation: start the shared store empty; each step registers its declarations as it builds.
            // Rebuilds (navigation, color change) reuse the existing store so values persist across screens.
            currentWorkflowStateStore = PaywallStateStore(emptyMap())
        }

        // Pre-compute the package step so its default package is available in cache
        // for early packageless steps to use as context.
        val stepWithPackages = workflow.singleStepFallbackId?.let { workflow.steps[it] }
        if (stepWithPackages != null && stepWithPackages.id != currentStep.id) {
            buildStateFromStep(
                stepWithPackages,
                workflow,
                offerings,
                presentedOfferingContext,
                shouldApplyState = false,
            )
        }

        buildStateFromStep(currentStep, workflow, offerings, presentedOfferingContext)
        if (isNewWorkflowImpression && _workflowState.value != null) {
            trackWorkflowStepStarted(
                step = currentStep,
                fromStepId = null,
                entryReason = WorkflowStepEntryReason.START,
            )
        }
        preWarmWorkflowStepCache(workflow, offerings, presentedOfferingContext)
    }

    private fun buildStateFromStep(
        step: WorkflowStep,
        workflow: PublishedWorkflow,
        offerings: Offerings,
        presentedOfferingContext: PresentedOfferingContext?,
        fromStepId: String? = null,
        navigationDirection: NavigationDirection? = null,
        shouldApplyState: Boolean = true,
    ) {
        val cached = workflowStepStateCache[step.id]
        val newState = cached ?: computeStateForStep(step, workflow, offerings, presentedOfferingContext)
        if (cached == null && newState is PaywallState.Loaded.Components) {
            workflowStepStateCache[step.id] = newState
        }
        // Apply the workflow's default package to all steps. setDefaultPackage is idempotent
        // so it is safe to call on every visit — it will only take effect the first time.
        // On steps with their own packages, ownSelection takes precedence over defaultPackageInfo.
        if (newState is PaywallState.Loaded.Components) {
            val defaultPackage = workflow.singleStepFallbackId
                ?.let { workflowStepStateCache[it]?.selectedPackageInfo }
            if (defaultPackage != null) {
                newState.setDefaultPackage(defaultPackage)
            }
        }
        if (!shouldApplyState) return
        currentWorkflowStepTracksPaywallEvents = newState is PaywallState.Loaded.Components &&
            step.tracksPaywallEvents(workflow)
        val pendingTransition = if (fromStepId != null && navigationDirection != null) {
            WorkflowPendingTransition(
                fromStepId = fromStepId,
                direction = navigationDirection,
                id = ++transitionIdCounter,
            )
        } else {
            null
        }
        // Set workflowState before _state so a recomposition that lands between the two writes
        // sees the workflow branch and the correct step, not the single-page branch.
        // On error, clear workflowState so the UI falls through to the normal error path rather
        // than entering workflow mode with a currentStepId absent from stepStates.
        if (newState !is PaywallState.Loaded.Components) {
            currentWorkflowStep?.let { currentStep ->
                trackWorkflowStepCompleted(step = currentStep, toStepId = null)
            }
        }
        _workflowState.value = if (newState is PaywallState.Loaded.Components) {
            WorkflowPaywallUiState(
                currentStepId = step.id,
                stepStates = workflowStepStateCache.toMap(),
                pendingTransition = pendingTransition,
            )
        } else {
            null
        }
        _state.value = newState
    }

    override fun onTransitionComplete(transitionId: Int) {
        val current = _workflowState.value ?: return
        if (current.pendingTransition?.id == transitionId) {
            _workflowState.value = current.copy(pendingTransition = null)
        }
    }

    @Suppress("ReturnCount")
    private fun computeStateForStep(
        step: WorkflowStep,
        workflow: PublishedWorkflow,
        offerings: Offerings,
        presentedOfferingContext: PresentedOfferingContext?,
    ): PaywallState {
        val screenId = step.screenId
            ?: return PaywallState.Error("Step '${step.id}' has no screen_id in workflow '${workflow.id}'")
        val screen = workflow.screens[screenId]
            ?: return PaywallState.Error("Screen '$screenId' not found in workflow '${workflow.id}'")
        val offeringId = screen.offeringIdentifier
            ?: return PaywallState.Error("Screen '$screenId' has no offering_id in workflow '${workflow.id}'")
        val baseOffering = offerings[offeringId]
            ?: return PaywallState.Error("Offering '$offeringId' not found for screen '$screenId'")

        val paywallComponents = WorkflowScreenMapper.toPaywallComponents(screen, screenId, workflow.uiConfig)
        val offering = Offering(
            identifier = baseOffering.identifier,
            serverDescription = baseOffering.serverDescription,
            metadata = baseOffering.metadata,
            availablePackages = baseOffering.availablePackages,
            paywallComponents = paywallComponents,
            webCheckoutURL = baseOffering.webCheckoutURL,
        )
        val offeringWithContext = presentedOfferingContext?.let { offering.copy(it) } ?: offering

        return calculateState(
            offering = offeringWithContext,
            colorScheme = _colorScheme.value,
            storefrontCountryCode = purchases.storefrontCountryCode,
            mode = options.mode,
            stateStore = currentWorkflowStateStore,
        )
    }

    /**
     * Eagerly computes and caches states for the remaining workflow steps off the main thread,
     * so that navigating to them doesn't block on the heavy [calculateState] work.
     */
    private fun preWarmWorkflowStepCache(
        workflow: PublishedWorkflow,
        offerings: Offerings,
        presentedOfferingContext: PresentedOfferingContext?,
    ) {
        preWarmJob = viewModelScope.launch {
            for ((stepId, step) in workflow.steps) {
                if (stepId in workflowStepStateCache) continue
                val computed = withContext(backgroundDispatcher) {
                    computeStateForStep(step, workflow, offerings, presentedOfferingContext)
                }
                if (computed is PaywallState.Loaded.Components && stepId !in workflowStepStateCache) {
                    workflowStepStateCache[stepId] = computed
                    computed.update(localeList = _lastLocaleList.value.toFrameworkLocaleList())
                    workflow.singleStepFallbackId
                        ?.let { workflowStepStateCache[it]?.selectedPackageInfo }
                        ?.let { computed.setDefaultPackage(it) }
                }
            }
            _workflowState.value = _workflowState.value?.copy(stepStates = workflowStepStateCache.toMap())
        }
    }

    @Suppress("ReturnCount")
    override fun handleWorkflowAction(componentId: String, triggerType: WorkflowTriggerType) {
        val navigator = workflowNavigator ?: return
        val result = currentWorkflowResult ?: return
        val offerings = currentWorkflowOfferings ?: return
        val candidate = navigator.peekTriggerStep(componentId, triggerType) ?: return
        validateStep(candidate, result.workflow, offerings)?.let { error ->
            Logger.e("Cannot navigate to step '${candidate.id}': $error")
            return
        }
        val fromStep = navigator.currentStep
        val fromStepId = fromStep?.id
        // triggerAction repeats the same lookup as peekTriggerStep. It should not return null
        // given the peek succeeded, but guard anyway to avoid a hard crash.
        val newStep = navigator.triggerAction(componentId, triggerType) ?: run {
            Logger.e("triggerAction returned null after peekTriggerStep succeeded — this is a bug")
            return
        }
        buildStateFromStep(
            newStep,
            result.workflow,
            offerings,
            currentWorkflowPresentedOfferingContext,
            fromStepId = fromStepId,
            navigationDirection = NavigationDirection.FORWARD,
        )
        trackWorkflowStepNavigation(
            fromStep = fromStep,
            toStep = newStep,
            entryReason = WorkflowStepEntryReason.FORWARD,
        )
    }

    @Suppress("ReturnCount")
    override fun handleBackNavigation(): Boolean {
        val navigator = workflowNavigator ?: return false
        if (!navigator.canNavigateBack) return false
        val result = currentWorkflowResult ?: return false
        val offerings = currentWorkflowOfferings ?: return false
        val candidate = navigator.peekBackStep ?: return false
        validateStep(candidate, result.workflow, offerings)?.let { error ->
            Logger.e("Cannot navigate back to step '${candidate.id}': $error")
            return false
        }
        val fromStep = navigator.currentStep
        val fromStepId = fromStep?.id
        // navigateBack should not return null given canNavigateBack is true, but guard to be safe.
        val newStep = navigator.navigateBack() ?: run {
            Logger.e("navigateBack returned null after canNavigateBack was true — this is a bug")
            return false
        }
        buildStateFromStep(
            newStep,
            result.workflow,
            offerings,
            currentWorkflowPresentedOfferingContext,
            fromStepId = fromStepId,
            navigationDirection = NavigationDirection.BACKWARD,
        )
        trackWorkflowStepNavigation(
            fromStep = fromStep,
            toStep = newStep,
            entryReason = WorkflowStepEntryReason.BACK,
        )
        return true
    }

    private fun trackWorkflowStepNavigation(
        fromStep: WorkflowStep?,
        toStep: WorkflowStep,
        entryReason: WorkflowStepEntryReason,
    ) {
        // If _workflowState is null after buildStateFromStep, an error occurred and
        // StepCompleted was already fired inside buildStateFromStep.
        if (_workflowState.value == null) return

        fromStep?.let { from ->
            trackWorkflowStepCompleted(step = from, toStepId = toStep.id)
        }
        trackWorkflowStepStarted(
            step = toStep,
            fromStepId = fromStep?.id,
            entryReason = entryReason,
        )
    }

    private fun trackWorkflowStepStarted(
        step: WorkflowStep,
        fromStepId: String?,
        entryReason: WorkflowStepEntryReason,
    ) {
        val workflowResult = currentWorkflowResult ?: return
        val workflow = workflowResult.workflow
        purchases.track(
            WorkflowEvent.StepStarted(
                creationData = WorkflowEvent.CreationData(UUID.randomUUID(), Date()),
                workflowId = workflow.id,
                stepId = step.id,
                traceId = workflowTraceId,
                fromStepId = fromStepId,
                entryReason = entryReason.value,
                isFirstStep = step.id == workflow.initialStepId,
                isLastStep = isTerminalStep(workflow, step.id),
            ),
        )
    }

    private fun trackWorkflowStepCompleted(step: WorkflowStep, toStepId: String?) {
        val workflowResult = currentWorkflowResult ?: return
        val workflow = workflowResult.workflow
        purchases.track(
            WorkflowEvent.StepCompleted(
                creationData = WorkflowEvent.CreationData(UUID.randomUUID(), Date()),
                workflowId = workflow.id,
                stepId = step.id,
                traceId = workflowTraceId,
                toStepId = toStepId,
                isFirstStep = step.id == workflow.initialStepId,
                isLastStep = isTerminalStep(workflow, step.id),
            ),
        )
    }

    private fun isTerminalStep(workflow: PublishedWorkflow, stepId: String): Boolean {
        val step = workflow.steps[stepId] ?: return false
        return step.triggerActions.values.none { it is WorkflowTriggerAction.Step }
    }

    private val currentWorkflowStep: WorkflowStep?
        get() {
            val stepId = _workflowState.value?.currentStepId ?: return null
            return currentWorkflowResult?.workflow?.steps?.get(stepId)
        }

    /**
     * Fires [WorkflowEvent.StepCompleted] for the step the user is currently on, if any. No-op for
     * non-workflow paywalls. Used when the current step is left without navigating to another one.
     * Called directly on dismiss paths that intentionally do not emit a paywall close event (a
     * successful purchase, and the REVENUECAT restore-dismiss), and from [closePaywall] (which
     * additionally emits the close event). This keeps paywall_close behavior identical to non-workflow.
     */
    private fun trackCurrentWorkflowStepCompleted() {
        currentWorkflowStep?.let { step ->
            trackWorkflowStepCompleted(step = step, toStepId = null)
        }
    }

    /**
     * Fires [WorkflowEvent.Close] for the step the user is currently on, if any. No-op for
     * non-workflow paywalls. This is the workflow-level abandonment signal: unlike paywall_close it is
     * not gated by [WorkflowStep.tracksPaywallEvents], so abandonment that happens on a non-paywall step
     * (e.g. before the offer is shown) is still captured. Only fired from [closePaywall] when the
     * workflow was not completed (no purchase).
     */
    private fun trackCurrentWorkflowAbandoned() {
        val workflowResult = currentWorkflowResult ?: return
        val step = currentWorkflowStep ?: return
        val workflow = workflowResult.workflow
        purchases.track(
            WorkflowEvent.Close(
                creationData = WorkflowEvent.CreationData(UUID.randomUUID(), Date()),
                workflowId = workflow.id,
                stepId = step.id,
                traceId = workflowTraceId,
                isFirstStep = step.id == workflow.initialStepId,
                isLastStep = isTerminalStep(workflow, step.id),
            ),
        )
    }

    @Suppress("ReturnCount")
    private fun validateStep(step: WorkflowStep, workflow: PublishedWorkflow, offerings: Offerings): String? {
        val screenId = step.screenId
            ?: return "Step '${step.id}' has no screen_id in workflow '${workflow.id}'"
        val screen = workflow.screens[screenId]
            ?: return "Screen '$screenId' not found in workflow '${workflow.id}'"
        val offeringId = screen.offeringIdentifier
            ?: return "Screen '$screenId' has no offering_id in workflow '${workflow.id}'"
        offerings[offeringId]
            ?: return "Offering '$offeringId' not found for screen '$screenId'"
        return null
    }

    /**
     * Whether this workflow step reports paywall events (`paywall_impression` / `paywall_close`),
     * driven by the backend `screen_type` tag (khepri #21429):
     * - tagged with `paywall` → reports;
     * - tagged without `paywall` (including an empty list) → suppressed;
     * - untagged (null `screen_type`, e.g. a pre-rollout/legacy payload) → falls back to the prior
     *   structural inference `id == singleStepFallbackId`, so untagged workflows behave exactly as
     *   before the `screen_type` rollout (only the fallback step reports) rather than over-reporting
     *   on every step.
     *
     * Once the backend republishes a workflow it tags exactly the fallback step as `["paywall"]`, so
     * tagged behavior is equivalent to the fallback while the explicit signal lets the backend classify
     * steps independently of `single_step_fallback_id`.
     */
    private fun WorkflowStep.tracksPaywallEvents(workflow: PublishedWorkflow): Boolean {
        val screenType = stepScreenType ?: return id == workflow.singleStepFallbackId
        return screenType.contains(WorkflowScreenType.PAYWALL)
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
        stateStore: PaywallStateStore? = null,
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
                validationWarning = validationResult.warning,
            )
            is PaywallValidationResult.Components -> offering.toComponentsPaywallState(
                validationResult = validationResult,
                storefrontCountryCode = storefrontCountryCode,
                dateProvider = { Date() },
                purchases = purchases,
                customVariables = options.customVariables,
                defaultCustomVariables = extractDefaultCustomVariables(offering),
                stateStore = stateStore,
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
        val productId = rcPackage.product.paywallProductIdentifier()
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
        val productId = rcPackage.product.paywallProductIdentifier()
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
    private fun createEventData(): PaywallEvent.Data? {
        val workflowId = currentWorkflowResult?.workflow?.id
        val stepId = _workflowState.value?.currentStepId
        return when (val currentState = state.value) {
            is PaywallState.Loaded.Legacy -> currentState.createEventData(workflowId, stepId)

            is PaywallState.Loaded.Components -> currentState.createEventData(workflowId, stepId)

            is PaywallState.Error,
            is PaywallState.Loading,
            -> {
                Logger.e("Unexpected state trying to create event data: $currentState")
                null
            }
        }
    }

    private fun PaywallState.Loaded.Legacy.createEventData(workflowId: String?, stepId: String?): PaywallEvent.Data? {
        val offering = offering
        val revision = this.offering.paywall?.revision ?: this.offering.paywallComponents?.data?.revision ?: run {
            Logger.e("Null paywall revision trying to create event data")
            return null
        }
        val paywallId = this.offering.paywall?.id ?: this.offering.paywallComponents?.data?.id
        val locale = _lastLocaleList.value.get(0) ?: Locale.getDefault()
        return PaywallEvent.Data(
            paywallIdentifier = paywallId,
            presentedOfferingContext = offering.presentedOfferingContextOrDefault,
            paywallRevision = revision,
            sessionIdentifier = UUID.randomUUID(),
            displayMode = mode.name.lowercase(),
            localeIdentifier = locale.toString(),
            darkMode = isDarkMode,
            workflowId = workflowId,
            stepId = stepId,
        )
    }

    private fun PaywallState.Loaded.Components.createEventData(
        workflowId: String?,
        stepId: String?,
    ): PaywallEvent.Data? {
        val offering = offering
        val paywallData = this.offering.paywallComponents ?: run {
            Logger.e("Null paywall revision trying to create event data")
            return null
        }
        return PaywallEvent.Data(
            paywallIdentifier = paywallData.data.id,
            presentedOfferingContext = offering.presentedOfferingContextOrDefault,
            paywallRevision = paywallData.data.revision,
            sessionIdentifier = UUID.randomUUID(),
            displayMode = mode.name.lowercase(),
            localeIdentifier = locale.toString(),
            darkMode = isDarkMode,
            workflowId = workflowId,
            stepId = stepId,
        )
    }

    private fun computePresentationFingerprint(): PaywallPresentationFingerprint? =
        when (val currentState = _state.value) {
            is PaywallState.Loaded.Legacy -> currentState.presentationFingerprintLegacy(
                mode = mode,
                localeList = _lastLocaleList.value,
                darkMode = isDarkMode,
            )
            is PaywallState.Loaded.Components -> currentState.presentationFingerprintComponents(
                mode = mode,
                darkMode = isDarkMode,
            )
            is PaywallState.Error,
            is PaywallState.Loading,
            -> null
        }

    private fun PaywallState.Loaded.Legacy.presentationFingerprintLegacy(
        mode: PaywallMode,
        localeList: LocaleListCompat,
        darkMode: Boolean,
    ): PaywallPresentationFingerprint? {
        val revision = offering.paywall?.revision
            ?: offering.paywallComponents?.data?.revision
            ?: return null
        val paywallId = offering.paywall?.id ?: offering.paywallComponents?.data?.id
        val locale = localeList.get(0) ?: Locale.getDefault()
        return PaywallPresentationFingerprint(
            paywallIdentifier = paywallId,
            presentedOfferingContext = offering.presentedOfferingContextOrDefault,
            paywallRevision = revision,
            displayMode = mode.name.lowercase(),
            localeIdentifier = locale.toString(),
            darkMode = darkMode,
        )
    }

    private fun PaywallState.Loaded.Components.presentationFingerprintComponents(
        mode: PaywallMode,
        darkMode: Boolean,
    ): PaywallPresentationFingerprint? {
        val paywallData = offering.paywallComponents ?: return null
        return PaywallPresentationFingerprint(
            paywallIdentifier = paywallData.data.id,
            presentedOfferingContext = offering.presentedOfferingContextOrDefault,
            paywallRevision = paywallData.data.revision,
            displayMode = mode.name.lowercase(),
            localeIdentifier = locale.toString(),
            darkMode = darkMode,
        )
    }

    private fun PaywallEvent.Data.presentationFingerprint(): PaywallPresentationFingerprint =
        PaywallPresentationFingerprint(
            paywallIdentifier = paywallIdentifier,
            presentedOfferingContext = presentedOfferingContext,
            paywallRevision = paywallRevision,
            displayMode = displayMode,
            localeIdentifier = localeIdentifier,
            darkMode = darkMode,
        )

    private fun PaywallEvent.Data.withCurrentWorkflowMetadata(): PaywallEvent.Data {
        val workflowId = currentWorkflowResult?.workflow?.id
        val stepId = _workflowState.value?.currentStepId
        return if (this.workflowId == workflowId && this.stepId == stepId) {
            this
        } else {
            copy(workflowId = workflowId, stepId = stepId)
        }
    }

    /**
     * Extracts default custom variable values from the offering's UiConfig.
     */
    private fun extractDefaultCustomVariables(offering: Offering): Map<String, CustomVariableValue> =
        offering.paywallComponents?.uiConfig?.customVariables
            ?.mapValues { (_, definition) -> CustomVariableValue.from(definition.defaultValue) }
            ?: emptyMap()

    private val Offering.presentedOfferingContextOrDefault: PresentedOfferingContext
        get() = presentedOfferingContext ?: PresentedOfferingContext(identifier)
}
