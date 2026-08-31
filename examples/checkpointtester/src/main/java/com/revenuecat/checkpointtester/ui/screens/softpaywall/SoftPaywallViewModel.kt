package com.revenuecat.checkpointtester.ui.screens.softpaywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPaywallOutcome
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.awaitCheckpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Soft paywall: the result never blocks anything, it only decides which banner the always-visible content gets.
 * This screen also shows the simplest possible call, without any CheckpointParams.
 */
class SoftPaywallViewModel : ViewModel() {

    data class UiState(
        val running: Boolean = false,
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
        if (_state.value.running) return
        _state.update { it.copy(running = true, message = null, hasRun = true) }
        viewModelScope.launch {
            try {
                val result = Purchases.sharedInstance.awaitCheckpoint("soft_paywall")
                when (result) {
                    is CheckpointResult.ReceivedOffering ->
                        free("Offering ${result.offering.identifier} returned for app-owned presentation.")
                    is CheckpointResult.PaywallPresented -> when (val outcome = result.paywallOutcome) {
                        is CheckpointPaywallOutcome.Purchased -> upgraded("Purchased.")
                        is CheckpointPaywallOutcome.Restored -> upgraded("Restored.")
                        CheckpointPaywallOutcome.Dismissed -> free("Dismissed, staying on the free tier.")
                        CheckpointPaywallOutcome.WebCheckoutOpened -> free("Left to pay via web checkout.")
                        is CheckpointPaywallOutcome.Error -> free("Paywall error: ${outcome.error.message}")
                        else -> free("Unknown paywall outcome.")
                    }
                    is CheckpointResult.NoAction -> free("No paywall shown (${result.reason}).")
                    else -> free("Unknown checkpoint result.")
                }
            } catch (e: PurchasesException) {
                // Even a failure is non-blocking here: the user keeps the free experience.
                free("Checkpoint failed: ${e.message}")
            }
        }
    }

    private fun upgraded(message: String) {
        _state.update { it.copy(running = false, upgraded = true, message = message) }
    }

    private fun free(message: String) {
        // A later dismissal doesn't take away an earlier purchase, so `upgraded` only ever moves forward.
        _state.update { it.copy(running = false, message = message) }
    }
}
