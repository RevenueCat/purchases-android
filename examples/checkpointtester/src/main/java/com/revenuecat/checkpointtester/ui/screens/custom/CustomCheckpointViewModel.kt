package com.revenuecat.checkpointtester.ui.screens.custom

import androidx.lifecycle.ViewModel
import com.revenuecat.checkpointtester.checkpoints.summary
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
        Purchases.sharedInstance.checkpoint(checkpointIdentifier) { gateResult ->
            _state.value = gateResult.toUiState()
        }
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun CheckpointGateResult.toUiState(): UiState {
        val error = error
        val noActionReason = noActionReason
        return when {
            error != null && noActionReason != null -> UiState(
                title = "Failed",
                detail = "${error.code}: ${error.message}",
                raw = toString(),
                isError = true,
            )
            noActionReason != null -> UiState(
                title = "Nothing served",
                detail = "Reason: $noActionReason",
                raw = toString(),
            )
            else -> UiState(
                title = "Workflow presented",
                detail = summary(),
                raw = toString(),
            )
        }
    }
}
