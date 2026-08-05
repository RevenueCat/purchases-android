package com.revenuecat.checkpointtester.ui.screens.hardpaywall

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
 * Hard paywall semantics: access is only granted when the checkpoint ends in Purchased or Restored. Every other
 * outcome, including a dismissal, leaves the content locked.
 */
class HardPaywallViewModel : ViewModel() {

    data class UiState(
        val waitingFor: String? = null,
        val result: CheckpointResultUi? = null,
        val unlocked: Boolean = false,
        val attempts: Int = 0,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun hitIfNeeded() {
        if (_state.value.attempts == 0) hit()
    }

    fun hit() {
        if (_state.value.waitingFor != null || _state.value.unlocked) return
        _state.update {
            it.copy(waitingFor = CHECKPOINT_IDENTIFIER, result = null, attempts = it.attempts + 1)
        }
        viewModelScope.launch {
            val result = CheckpointRunner.run(
                CHECKPOINT_IDENTIFIER,
                "gate" to "hard",
                "attempt" to _state.value.attempts,
            )
            _state.update {
                it.copy(waitingFor = null, result = result, unlocked = result.grantedAccess)
            }
        }
    }

    private companion object {
        const val CHECKPOINT_IDENTIFIER = "hard_paywall"
    }
}
