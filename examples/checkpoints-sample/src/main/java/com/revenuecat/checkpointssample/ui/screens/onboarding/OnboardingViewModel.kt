package com.revenuecat.checkpointssample.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import com.revenuecat.checkpointssample.Constants
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OnboardingViewModel : ViewModel() {

    enum class Step(val title: String, val body: String, val buttonLabel: String) {
        Welcome(
            title = "Welcome to Checkpoints Arcade",
            body = "A tiny game you can play once you unlock it. Let's get you set up first.",
            buttonLabel = "Continue",
        ),
        Personalize(
            title = "Make it yours",
            body = "Pretend you picked your favorite game genre here. " +
                "Finishing runs the onboarding checkpoint before landing on home.",
            buttonLabel = "Get started",
        ),
    }

    data class UiState(
        val step: Step = Step.Welcome,
        val finishing: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun advance(onFinished: () -> Unit) {
        if (_state.value.finishing) return
        when (_state.value.step) {
            Step.Welcome -> _state.update { it.copy(step = Step.Personalize) }
            Step.Personalize -> finish(onFinished)
        }
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun finish(onFinished: () -> Unit) {
        _state.update { it.copy(finishing = true) }
        Purchases.sharedInstance.checkpoint(Constants.ONBOARDING_CHECKPOINT_ID) {
            // Whatever the checkpoint served (or didn't), onboarding is over: continue to home.
            _state.update { state -> state.copy(finishing = false) }
            onFinished()
        }
    }
}
