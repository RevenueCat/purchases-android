package com.revenuecat.checkpointtester.ui.screens.gate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.checkpointtester.checkpoints.CheckpointResultUi
import com.revenuecat.checkpointtester.checkpoints.CheckpointRunner
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The closest thing here to a real integration: the app checks entitlements first and only reaches the
 * checkpoint when the user has nothing active, then re-reads [com.revenuecat.purchases.CustomerInfo] so a
 * purchase made on the checkpoint paywall visibly flips the gate.
 */
class EntitlementGateViewModel : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val activeEntitlements: List<String> = emptyList(),
        val customerInfoError: String? = null,
        val waitingFor: String? = null,
        val result: CheckpointResultUi? = null,
        val checkpointSkipped: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun refresh() {
        if (_state.value.loading || _state.value.waitingFor != null) return
        _state.update { it.copy(loading = true, customerInfoError = null, checkpointSkipped = false) }
        viewModelScope.launch {
            val active = fetchActiveEntitlements(CacheFetchPolicy.FETCH_CURRENT) ?: return@launch
            if (active.isNotEmpty()) {
                _state.update { it.copy(loading = false, activeEntitlements = active, checkpointSkipped = true) }
                return@launch
            }
            _state.update {
                it.copy(
                    loading = false,
                    activeEntitlements = active,
                    waitingFor = CHECKPOINT_IDENTIFIER,
                    result = null,
                )
            }
            val result = CheckpointRunner.run(CHECKPOINT_IDENTIFIER, "gate" to "entitlement")
            _state.update { it.copy(waitingFor = null, result = result) }
            // Re-read after the paywall so a purchase or restore shows up in the entitlement list.
            fetchActiveEntitlements(CacheFetchPolicy.FETCH_CURRENT)?.let { updated ->
                _state.update { it.copy(activeEntitlements = updated) }
            }
        }
    }

    private suspend fun fetchActiveEntitlements(fetchPolicy: CacheFetchPolicy): List<String>? = try {
        Purchases.sharedInstance.awaitCustomerInfo(fetchPolicy).entitlements.active.keys.sorted()
    } catch (e: PurchasesException) {
        _state.update { it.copy(loading = false, customerInfoError = "${e.code}: ${e.message}") }
        null
    }

    private companion object {
        const val CHECKPOINT_IDENTIFIER = "entitlement_gate"
    }
}
