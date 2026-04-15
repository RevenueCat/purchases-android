@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import android.app.Activity
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.utils.appendQueryParameter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URI
import java.net.URISyntaxException
import java.util.Date
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result as RcResult

internal interface WorkflowViewModel {
    val state: StateFlow<WorkflowState>
    val actionInProgress: State<Boolean>
    val actionError: State<PurchasesError?>

    suspend fun handlePackagePurchase(activity: Activity, pkg: Package?, resolvedOffer: ResolvedOffer?)
    suspend fun handleRestorePurchases()
    fun getWebCheckoutUrl(action: PaywallAction.External.LaunchWebCheckout): String?
    fun invalidateCustomerInfoCache()
    fun clearActionError()
}

internal class WorkflowViewModelImpl(
    private val workflowId: String,
    private val purchases: PurchasesType,
    private val resourceProvider: ResourceProvider,
) : ViewModel(), WorkflowViewModel {

    private val _state = MutableStateFlow<WorkflowState>(WorkflowState.Loading)
    override val state: StateFlow<WorkflowState> = _state.asStateFlow()

    private val _actionInProgress = mutableStateOf(false)
    override val actionInProgress: State<Boolean> = _actionInProgress

    private val _actionError = mutableStateOf<PurchasesError?>(null)
    override val actionError: State<PurchasesError?> = _actionError

    init {
        loadWorkflow()
    }

    @Suppress("LongMethod")
    private fun loadWorkflow() {
        viewModelScope.launch {
            try {
                val fetchResult = purchases.awaitGetWorkflow(workflowId)
                val workflow = fetchResult.workflow

                val step = workflow.steps[workflow.initialStepId]
                if (step == null) {
                    _state.value = WorkflowState.Error(
                        "Initial step '${workflow.initialStepId}' not found in workflow '${workflow.id}'",
                    )
                    return@launch
                }

                val screenId = step.screenId
                if (screenId == null) {
                    _state.value = WorkflowState.Error(
                        "Initial step '${step.id}' has no screen_id in workflow '${workflow.id}'",
                    )
                    return@launch
                }

                val screen = workflow.screens[screenId]
                if (screen == null) {
                    _state.value = WorkflowState.Error(
                        "Screen '$screenId' not found in workflow '${workflow.id}'",
                    )
                    return@launch
                }

                val paywallComponents = WorkflowScreenMapper.toPaywallComponents(screen, workflow.uiConfig)

                // TODO: backend is sending internal IDs instead of public identifiers; remove once fixed
                val offeringId = if (screen.offeringId?.startsWith("ofrnge") == true) "rosie_duped" else screen.offeringId
                if (offeringId == null) {
                    _state.value = WorkflowState.Error(
                        "Screen '$screenId' has no offering_id in workflow '${workflow.id}'",
                    )
                    return@launch
                }

                val offerings = purchases.awaitOfferings()
                val baseOffering = offerings[offeringId]
                if (baseOffering == null) {
                    _state.value = WorkflowState.Error(
                        "Offering '$offeringId' not found for screen '$screenId'",
                    )
                    return@launch
                }

                val offering = Offering(
                    identifier = baseOffering.identifier,
                    serverDescription = baseOffering.serverDescription,
                    metadata = baseOffering.metadata,
                    availablePackages = baseOffering.availablePackages,
                    paywallComponents = paywallComponents,
                    webCheckoutURL = baseOffering.webCheckoutURL,
                )

                val validationResult = offering.validatePaywallComponentsDataOrNull(resourceProvider)
                if (validationResult == null) {
                    _state.value = WorkflowState.Error(
                        "Paywall components not found after attaching screen data",
                    )
                    return@launch
                }

                when (validationResult) {
                    is RcResult.Error -> {
                        _state.value = WorkflowState.Error(
                            "Paywall validation failed: ${validationResult.value.joinToString { it.toString() }}",
                        )
                    }
                    is RcResult.Success -> {
                        val paywallState = offering.toComponentsPaywallState(
                            validationResult = validationResult.value,
                            storefrontCountryCode = purchases.storefrontCountryCode,
                            dateProvider = { Date() },
                            purchases = purchases,
                        )
                        _state.value = WorkflowState.Loaded(paywallState)
                    }
                }
            } catch (e: PurchasesException) {
                _state.value = WorkflowState.Error("Error ${e.code.code}: ${e.code.description}")
            }
        }
    }

    override suspend fun handlePackagePurchase(activity: Activity, pkg: Package?, resolvedOffer: ResolvedOffer?) {
        if (pkg == null) {
            Logger.e("WorkflowView: no package for purchase action")
            return
        }
        if (_actionInProgress.value) return
        _actionInProgress.value = true
        try {
            val subscriptionOption = resolvedOffer?.subscriptionOption
            val paramsBuilder = if (subscriptionOption != null) {
                PurchaseParams.Builder(activity, subscriptionOption)
                    .presentedOfferingContext(pkg.presentedOfferingContext)
            } else {
                PurchaseParams.Builder(activity, pkg)
            }
            purchases.awaitPurchase(paramsBuilder)
        } catch (e: PurchasesException) {
            Logger.e("WorkflowView: purchase failed: $e")
            _actionError.value = e.error
        } finally {
            _actionInProgress.value = false
        }
    }

    override suspend fun handleRestorePurchases() {
        if (_actionInProgress.value) return
        _actionInProgress.value = true
        try {
            purchases.awaitRestore()
        } catch (e: PurchasesException) {
            Logger.e("WorkflowView: restore failed: $e")
            _actionError.value = e.error
        } finally {
            _actionInProgress.value = false
        }
    }

    @SuppressWarnings("ReturnCount")
    override fun getWebCheckoutUrl(action: PaywallAction.External.LaunchWebCheckout): String? {
        val customUrl = action.customUrl
        val loadedState = _state.value as? WorkflowState.Loaded
        val behavior = action.packageParamBehavior
        val (packageToUse, packageParam) = when (behavior) {
            is PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.Append -> {
                val pkg = behavior.rcPackage
                    ?: loadedState?.paywallState?.selectedPackageInfo?.rcPackage
                pkg to behavior.packageParam
            }
            is PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.DoNotAppend ->
                null to null
        }
        if (customUrl != null) {
            val uri = try {
                URI(customUrl)
            } catch (e: URISyntaxException) {
                Logger.e("WorkflowView: invalid custom URI: $customUrl", e)
                return null
            }
            return if (packageParam != null && packageToUse != null) {
                uri.appendQueryParameter(packageParam, packageToUse.identifier).toString()
            } else {
                uri.toString()
            }
        }
        return loadedState?.paywallState?.offering?.webCheckoutURL?.toString()
    }

    override fun invalidateCustomerInfoCache() {
        // no-op in M2 — full implementation deferred to PaywallView migration
    }

    override fun clearActionError() {
        _actionError.value = null
    }
}
