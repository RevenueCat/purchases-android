package com.revenuecat.checkpointtester.ui.screens.gate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPaywallOutcome
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.awaitCheckpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The closest thing here to a real integration: the app checks entitlements first and only reaches the checkpoint
 * when nothing is active. A purchase or restore carries its own [CustomerInfo] on the outcome, so the refreshed
 * entitlements come straight off the result with no second fetch.
 */
class EntitlementGateViewModel : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val activeEntitlements: List<String> = emptyList(),
        val customerInfoError: String? = null,
        val running: Boolean = false,
        val message: String? = null,
        val checkpointSkipped: Boolean = false,
        val hasRun: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun refreshIfNeeded() {
        if (!_state.value.hasRun) refresh()
    }

    @OptIn(InternalRevenueCatAPI::class)
    fun refresh() {
        if (_state.value.loading || _state.value.running) return
        _state.update {
            it.copy(loading = true, customerInfoError = null, checkpointSkipped = false, hasRun = true)
        }
        viewModelScope.launch {
            val customerInfo = try {
                Purchases.sharedInstance.awaitCustomerInfo(CacheFetchPolicy.FETCH_CURRENT)
            } catch (e: PurchasesException) {
                _state.update { it.copy(loading = false, customerInfoError = "${e.code}: ${e.message}") }
                return@launch
            }

            val active = customerInfo.activeEntitlements()
            if (active.isNotEmpty()) {
                _state.update {
                    it.copy(loading = false, activeEntitlements = active, checkpointSkipped = true)
                }
                return@launch
            }

            _state.update {
                it.copy(loading = false, activeEntitlements = active, running = true, message = null)
            }
            try {
                val result = Purchases.sharedInstance.awaitCheckpoint(
                    "entitlement_gate",
                    CheckpointParams { customVariables { "gate" to "entitlement" } },
                )
                handleCheckpointResult(result)
            } catch (e: PurchasesException) {
                finish("Checkpoint failed: ${e.message}")
            }
        }
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun handleCheckpointResult(result: CheckpointResult) {
        when (result) {
            is CheckpointResult.ReceivedOffering ->
                finish("Offering ${result.offering.identifier} returned; the app should present it.")
            is CheckpointResult.PaywallPresented -> handlePaywallOutcome(result.paywallOutcome)
            is CheckpointResult.NoAction -> finish("No paywall shown (${result.reason.value}).")
            else -> finish("Unknown checkpoint result.")
        }
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun handlePaywallOutcome(outcome: CheckpointPaywallOutcome) {
        when (outcome) {
            // The outcome carries the up-to-date CustomerInfo, so there's no need to fetch again.
            is CheckpointPaywallOutcome.Purchased -> granted("Purchased.", outcome.customerInfo)
            is CheckpointPaywallOutcome.Restored -> granted("Restored.", outcome.customerInfo)
            CheckpointPaywallOutcome.Dismissed -> finish("Dismissed, still no entitlement.")
            is CheckpointPaywallOutcome.Error -> finish("Paywall error: ${outcome.error.message}")
            else -> finish("Unknown paywall outcome.")
        }
    }

    private fun granted(message: String, customerInfo: CustomerInfo) {
        _state.update {
            it.copy(running = false, message = message, activeEntitlements = customerInfo.activeEntitlements())
        }
    }

    private fun finish(message: String) {
        _state.update { it.copy(running = false, message = message) }
    }

    private fun CustomerInfo.activeEntitlements(): List<String> = entitlements.active.keys.sorted()
}
