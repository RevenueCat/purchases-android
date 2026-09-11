package com.revenuecat.checkpointtester.ui.screens.hardpaywall

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
 * Hard paywall: access is granted only when the user obtains an entitlement through the checkpoint. Every other
 * outcome, a dismissal included, leaves the content locked and lets the user try again. Backing out of the flow
 * never reaches the callback, so nothing here waits on it.
 */
class HardPaywallViewModel : ViewModel() {

    data class UiState(
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
        if (_state.value.unlocked) return
        _state.update { it.copy(message = null, attempts = it.attempts + 1) }
        Purchases.sharedInstance.checkpoint(
            "hard_paywall",
            PaywallPresenters.params {
                customVariables {
                    "gate" to "hard"
                    "attempt" to _state.value.attempts
                }
            },
        ) { result ->
            if (result != null && result.obtainedEntitlements.isNotEmpty()) {
                unlock("${result.summary()} Access granted.")
            } else {
                stayLocked(result.summary())
            }
        }
    }

    private fun unlock(message: String) {
        _state.update { it.copy(unlocked = true, message = message) }
    }

    private fun stayLocked(message: String) {
        _state.update { it.copy(unlocked = false, message = message) }
    }
}
