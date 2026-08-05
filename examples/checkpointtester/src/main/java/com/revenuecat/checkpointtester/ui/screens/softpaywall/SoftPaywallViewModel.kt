package com.revenuecat.checkpointtester.ui.screens.softpaywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.checkpointtester.checkpoints.CheckpointResultUi
import com.revenuecat.checkpointtester.checkpoints.CheckpointRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Soft paywall semantics: the checkpoint result never blocks anything. It only decides which banner the
 * always-visible content is wrapped in.
 */
class SoftPaywallViewModel : ViewModel() {

    data class UiState(
        val waitingFor: String? = null,
        val result: CheckpointResultUi? = null,
        val hasRun: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun hitIfNeeded() {
        if (!_state.value.hasRun) hit()
    }

    fun hit() {
        if (_state.value.waitingFor != null) return
        _state.update { it.copy(waitingFor = CHECKPOINT_IDENTIFIER, result = null, hasRun = true) }
        viewModelScope.launch {
            val result = CheckpointRunner.run(CHECKPOINT_IDENTIFIER, "gate" to "soft")
            _state.update { it.copy(waitingFor = null, result = result) }
        }
    }

    private companion object {
        const val CHECKPOINT_IDENTIFIER = "soft_paywall"
    }
}
