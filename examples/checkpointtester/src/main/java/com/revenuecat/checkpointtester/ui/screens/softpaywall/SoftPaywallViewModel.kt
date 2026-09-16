package com.revenuecat.checkpointtester.ui.screens.softpaywall

import androidx.lifecycle.ViewModel
import com.revenuecat.checkpointtester.checkpoints.PaywallPresenters
import com.revenuecat.checkpointtester.checkpoints.summary
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Soft paywall: the result never blocks anything, it only decides which banner the always-visible content gets.
 * This screen also shows the simplest call: no custom variables, only the tester's presenter selection.
 */
class SoftPaywallViewModel : ViewModel() {

    data class UiState(
        val upgraded: Boolean = false,
        val message: String? = null,
        val hasRun: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun hitIfNeeded() {
        if (!_state.value.hasRun) hit()
    }

    @OptIn(InternalRevenueCatAPI::class)
    fun hit() {
        _state.update { it.copy(message = null, hasRun = true) }
        Purchases.sharedInstance.checkpoint("soft_paywall", PaywallPresenters.params()) { result ->
            if (result != null && result.obtainedEntitlements.isNotEmpty()) {
                upgraded(result.summary())
            } else {
                free(result.summary())
            }
        }
    }

    private fun upgraded(message: String) {
        _state.update { it.copy(upgraded = true, message = message) }
    }

    private fun free(message: String) {
        // A later dismissal doesn't take away an earlier purchase, so `upgraded` only ever moves forward.
        _state.update { it.copy(message = message) }
    }
}
