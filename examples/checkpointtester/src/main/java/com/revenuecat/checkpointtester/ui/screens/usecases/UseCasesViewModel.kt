package com.revenuecat.checkpointtester.ui.screens.usecases

import androidx.lifecycle.ViewModel
import com.revenuecat.checkpointtester.checkpoints.summary
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Runs identifiers with deterministic resolutions: an offering (presents the fallback paywall), an unknown
 * checkpoint, and a simulated error.
 */
class UseCasesViewModel : ViewModel() {

    data class UiState(
        val running: Boolean = false,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    @OptIn(InternalRevenueCatAPI::class)
    fun hit(identifier: String) {
        if (_state.value.running) return
        _state.update { it.copy(running = true, message = null) }
        Purchases.sharedInstance.checkpoint(identifier) { gateResult ->
            _state.update { it.copy(running = false, message = gateResult.summary()) }
        }
    }
}
