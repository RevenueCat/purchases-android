package com.revenuecat.checkpointtester.ui.screens.hardpaywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
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
 * Hard paywall: access is granted only when the checkpoint ends in Purchased or Restored. Every other outcome,
 * a dismissal included, leaves the content locked and lets the user try again.
 */
class HardPaywallViewModel : ViewModel() {

    data class UiState(
        val running: Boolean = false,
        val unlocked: Boolean = false,
        val message: String? = null,
        val attempts: Int = 0,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun hitIfNeeded() {
        if (_state.value.attempts == 0) hit()
    }

    @OptIn(InternalRevenueCatAPI::class)
    fun hit() {
        if (_state.value.running || _state.value.unlocked) return
        _state.update { it.copy(running = true, message = null, attempts = it.attempts + 1) }
        viewModelScope.launch {
            try {
                val result = Purchases.sharedInstance.awaitCheckpoint(
                    "hard_paywall",
                    CheckpointParams {
                        customVariables {
                            "gate" to "hard"
                            "attempt" to _state.value.attempts
                        }
                    },
                )
                when (result) {
                    is CheckpointResult.ReceivedOffering -> stayLocked(
                        "Offering ${result.offering.identifier} returned; app-owned UI is required.",
                    )
                    is CheckpointResult.PaywallPresented -> when (val outcome = result.paywallOutcome) {
                        is CheckpointPaywallOutcome.Purchased -> unlock("Purchased. Access granted.")
                        is CheckpointPaywallOutcome.Restored -> unlock("Restored. Access granted.")
                        CheckpointPaywallOutcome.Dismissed -> stayLocked("Dismissed without purchasing.")
                        is CheckpointPaywallOutcome.Error -> stayLocked("Paywall error: ${outcome.error.message}")
                        else -> stayLocked("Unknown paywall outcome.")
                    }
                    // Nothing was served, so a hard gate has to keep the content locked.
                    is CheckpointResult.NoAction -> stayLocked("No paywall to show (${result.reason.value}).")
                    else -> stayLocked("Unknown checkpoint result.")
                }
            } catch (e: PurchasesException) {
                stayLocked("Checkpoint failed: ${e.message}")
            }
        }
    }

    private fun unlock(message: String) {
        _state.update { it.copy(running = false, unlocked = true, message = message) }
    }

    private fun stayLocked(message: String) {
        _state.update { it.copy(running = false, unlocked = false, message = message) }
    }
}
