package com.revenuecat.checkpointtester.ui.screens.onboarding

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
 * Onboarding semantics: the checkpoint runs between the last input step and the final step, and the flow always
 * advances afterwards regardless of the outcome. The result is surfaced on the final step for inspection.
 */
class OnboardingViewModel : ViewModel() {

    enum class Step(val title: String, val body: String) {
        Welcome(
            title = "Welcome",
            body = "A short onboarding flow that hits a checkpoint before the last step.",
        ),
        Personalize(
            title = "Personalize",
            body = "Pretend the user picked their preferences here. Continuing runs the checkpoint.",
        ),
        Done(
            title = "You're ready",
            body = "Onboarding completed. The checkpoint result is shown below.",
        ),
    }

    data class UiState(
        val step: Step = Step.Welcome,
        val waitingFor: String? = null,
        val result: CheckpointResultUi? = null,
    ) {
        val progress: Float
            get() = (step.ordinal + 1).toFloat() / Step.entries.size
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun next() {
        if (_state.value.waitingFor != null) return
        when (_state.value.step) {
            Step.Welcome -> _state.update { it.copy(step = Step.Personalize) }
            Step.Personalize -> runCheckpointThenFinish()
            Step.Done -> Unit
        }
    }

    fun previous() {
        if (_state.value.waitingFor != null) return
        val previousStep = Step.entries.getOrNull(_state.value.step.ordinal - 1) ?: return
        _state.update { it.copy(step = previousStep) }
    }

    fun restart() {
        if (_state.value.waitingFor != null) return
        _state.update { UiState() }
    }

    private fun runCheckpointThenFinish() {
        _state.update { it.copy(waitingFor = CHECKPOINT_IDENTIFIER, result = null) }
        viewModelScope.launch {
            val result = CheckpointRunner.run(CHECKPOINT_IDENTIFIER, "step" to Step.Personalize.name)
            _state.update { it.copy(waitingFor = null, result = result, step = Step.Done) }
        }
    }

    private companion object {
        const val CHECKPOINT_IDENTIFIER = "onboarding_complete"
    }
}
