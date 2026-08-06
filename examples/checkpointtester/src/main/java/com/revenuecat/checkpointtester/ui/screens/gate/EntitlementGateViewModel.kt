package com.revenuecat.checkpointtester.ui.screens.gate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCheckpoint
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.checkpoints.CheckpointParams
import com.revenuecat.purchases.checkpoints.CheckpointPaywallOutcome
import com.revenuecat.purchases.checkpoints.CheckpointResult
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
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    @OptIn(InternalRevenueCatAPI::class)
    fun refresh() {
        if (_state.value.loading || _state.value.running) return
        _state.update { it.copy(loading = true, customerInfoError = null, checkpointSkipped = false) }
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
                    CheckpointParams("gate" to "entitlement"),
                )
                when (result) {
                    is CheckpointResult.PaywallPresented -> when (val outcome = result.paywallOutcome) {
                        // The outcome carries the up-to-date CustomerInfo, so there's no need to fetch again.
                        is CheckpointPaywallOutcome.Purchased ->
                            granted("Purchased.", outcome.customerInfo)
                        is CheckpointPaywallOutcome.Restored ->
                            granted("Restored.", outcome.customerInfo)
                        CheckpointPaywallOutcome.Dismissed -> finish("Dismissed, still no entitlement.")
                        is CheckpointPaywallOutcome.Error -> finish("Paywall error: ${outcome.error.message}")
                        else -> finish("Unknown paywall outcome.")
                    }
                    is CheckpointResult.NoAction -> finish("No paywall shown (${result.reason.value}).")
                    else -> finish("Unknown checkpoint result.")
                }
            } catch (e: PurchasesException) {
                finish("Checkpoint failed: ${e.message}")
            }
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
