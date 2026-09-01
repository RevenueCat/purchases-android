package com.revenuecat.checkpointtester.ui.screens.gate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.checkpointtester.checkpoints.summary
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The closest thing here to a real integration: the app checks entitlements first and only reaches the checkpoint
 * when nothing is active. The gate result carries the entitlements the user obtained, so the displayed list is
 * updated straight from the grants with no second fetch.
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
            Purchases.sharedInstance.checkpoint(
                "entitlement_gate",
                CheckpointParams { customVariables { "gate" to "entitlement" } },
            ) { gateResult ->
                val granted = gateResult.entitlements.map { grant -> grant.identifier }
                _state.update {
                    it.copy(
                        running = false,
                        message = gateResult.summary(),
                        activeEntitlements = (it.activeEntitlements + granted).distinct().sorted(),
                    )
                }
            }
        }
    }

    private fun CustomerInfo.activeEntitlements(): List<String> = entitlements.active.keys.sorted()
}
