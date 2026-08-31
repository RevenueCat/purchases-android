package com.revenuecat.checkpointtester.ui.screens.custom

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
 * Runs whatever identifier is typed in, so a checkpoint configured in the dashboard can be tried without
 * rebuilding the app. Nothing is gated on the outcome: the result is reported as-is, including the raw
 * `toString()`, which is the point of this screen.
 */
class CustomCheckpointViewModel : ViewModel() {

    data class UiState(
        val runningFor: String? = null,
        val title: String? = null,
        val detail: String? = null,
        val raw: String? = null,
        val isError: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    @OptIn(InternalRevenueCatAPI::class)
    fun hit(identifier: String) {
        val checkpointIdentifier = identifier.trim()
        if (checkpointIdentifier.isEmpty() || _state.value.runningFor != null) return
        _state.update { UiState(runningFor = checkpointIdentifier) }
        viewModelScope.launch {
            val result = try {
                when (val result = Purchases.sharedInstance.awaitCheckpoint(checkpointIdentifier)) {
                    is CheckpointResult.ReceivedOffering -> UiState(
                        title = "Offering returned",
                        detail = "Identifier: ${result.offering.identifier}. The app now owns presentation.",
                        raw = result.toString(),
                    )
                    is CheckpointResult.PaywallPresented -> UiState(
                        title = "Paywall presented",
                        detail = when (val outcome = result.paywallOutcome) {
                            is CheckpointPaywallOutcome.Purchased -> "Purchased."
                            is CheckpointPaywallOutcome.Restored -> "Restored."
                            CheckpointPaywallOutcome.Dismissed -> "Dismissed."
                            CheckpointPaywallOutcome.WebCheckoutOpened -> "Left to pay via web checkout."
                            is CheckpointPaywallOutcome.Error -> "Paywall error: ${outcome.error.message}"
                            else -> "Unknown paywall outcome."
                        },
                        raw = result.toString(),
                    )
                    is CheckpointResult.NoAction -> UiState(
                        title = "No action",
                        detail = "Reason: ${result.reason}",
                        raw = result.toString(),
                    )
                    else -> UiState(title = "Unknown checkpoint result.", raw = result.toString())
                }
            } catch (e: PurchasesException) {
                UiState(title = "Failed", detail = "${e.code}: ${e.message}", raw = e.error.toString(), isError = true)
            }
            _state.value = result
        }
    }
}
