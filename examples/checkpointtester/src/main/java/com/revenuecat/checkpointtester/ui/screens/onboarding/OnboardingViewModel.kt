package com.revenuecat.checkpointtester.ui.screens.onboarding

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
 * Onboarding semantics: the checkpoint runs between the last input step and the final step, and the flow advances
 * whenever the checkpoint reports back, regardless of the outcome. Backing out of the flow never reports, so the user
 * stays on the last input step and can continue again. The result is surfaced on the final step for inspection.
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
        val message: String? = null,
    ) {
        val progress: Float
            get() = (step.ordinal + 1).toFloat() / Step.entries.size
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun next() {
        when (_state.value.step) {
            Step.Welcome -> _state.update { it.copy(step = Step.Personalize) }
            Step.Personalize -> runCheckpointThenFinish()
            Step.Done -> Unit
        }
    }

    fun previous() {
        val previousStep = Step.entries.getOrNull(_state.value.step.ordinal - 1) ?: return
        _state.update { it.copy(step = previousStep) }
    }

    fun restart() {
        _state.update { UiState() }
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun runCheckpointThenFinish() {
        _state.update { it.copy(message = null) }
        Purchases.sharedInstance.checkpoint(
            "onboarding_complete",
            PaywallPresenters.params { customVariables { "step" to Step.Personalize.name } },
        ) { result ->
            // Whatever happened, onboarding completes: a flow outcome must not strand the user mid-flow.
            _state.update { it.copy(message = result.summary(), step = Step.Done) }
        }
    }
}
