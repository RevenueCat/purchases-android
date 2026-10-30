package com.revenuecat.checkpointtester.ui.screens.hardpaywall

import androidx.lifecycle.ViewModel
import com.revenuecat.checkpointtester.checkpoints.PaywallPresenters
import com.revenuecat.checkpointtester.checkpoints.summary
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The checkpoint callback grants access. Remote hard gating suppresses the callback until any configured
 * entitlement is active. Backing out of an unresolved hard gate leaves the content locked and lets the user try again.
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

    @OptIn(InviteOnlyCheckpointsAPI::class)
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
            unlock("${result.summary()} Access granted.")
        }
    }

    private fun unlock(message: String) {
        _state.update { it.copy(unlocked = true, message = message) }
    }
}
