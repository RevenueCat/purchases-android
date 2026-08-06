package com.revenuecat.checkpointtester.ui.screens.usecases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCheckpoint
import com.revenuecat.purchases.checkpoints.CheckpointResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Runs the two identifiers that resolve deterministically, so neither is expected to present a paywall.
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
        viewModelScope.launch {
            val message = try {
                when (val result = Purchases.sharedInstance.awaitCheckpoint(identifier)) {
                    is CheckpointResult.NoAction -> "No action (${result.reason.value})."
                    is CheckpointResult.PaywallPresented -> "Unexpectedly presented a paywall."
                    else -> "Unknown checkpoint result."
                }
            } catch (e: PurchasesException) {
                "Failed: ${e.message}"
            }
            _state.update { it.copy(running = false, message = message) }
        }
    }
}
