package com.revenuecat.checkpointtester.ui.screens.custom

import androidx.lifecycle.ViewModel
import com.revenuecat.checkpointtester.checkpoints.summary
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult
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
        Purchases.sharedInstance.checkpoint(checkpointIdentifier) { result ->
            _state.value = result.toUiState()
        }
    }

    // Why nothing was presented, and any failure, only reach the logs.
    @OptIn(InternalRevenueCatAPI::class)
    private fun FlowResult?.toUiState(): UiState = if (this == null) {
        UiState(title = "Nothing presented", detail = "See the logs for the reason.", raw = "null")
    } else {
        UiState(title = "Flow presented", detail = summary(), raw = toString())
    }
}
