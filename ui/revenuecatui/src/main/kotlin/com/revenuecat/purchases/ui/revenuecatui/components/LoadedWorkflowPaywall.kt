@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.WorkflowPaywallUiState
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.workflow.NavigationDirection

@Composable
internal fun LoadedWorkflowPaywall(
    workflowState: WorkflowPaywallUiState,
    navigationDirection: NavigationDirection,
    clickHandler: suspend (PaywallAction.External) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
    modifier: Modifier = Modifier,
) {
    val currentStepId = workflowState.currentStepId
    val stepStates = workflowState.stepStates
    val currentState = stepStates[currentStepId] ?: run {
        Logger.e("Workflow step '$currentStepId' not found in stepStates — rendering nothing")
        return
    }

    val configuration = LocalConfiguration.current
    currentState.update(localeList = configuration.locales)

    val slideState = rememberWorkflowSlideState(
        workflowState = workflowState,
        navigationDirection = navigationDirection,
    )

    PaywallComponentsScaffold(
        state = currentState,
        clickHandler = clickHandler,
        componentInteractionTracker = componentInteractionTracker,
        modifier = modifier,
    ) { hasHeaderOverlay ->
        // Multi-step slide container: all composed steps are stacked and translated by workflowSlide.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            for (stepId in slideState.composedStepIds) {
                val stepState = stepStates[stepId] ?: continue
                key(stepId) {
                    WorkflowStepBody(
                        stepId = stepId,
                        stepState = stepState,
                        isCurrent = stepId == currentStepId,
                        currentStepId = currentStepId,
                        headerOffsetState = currentState,
                        hasHeaderOverlay = hasHeaderOverlay,
                        navigationDirection = navigationDirection,
                        slideState = slideState,
                        clickHandler = clickHandler,
                        componentInteractionTracker = componentInteractionTracker,
                    )
                }
            }
        }
    }
}

/**
 * Renders one step's main scrollable content with the slide-animation modifier applied.
 * Off-screen (parked) steps still receive a click handler, but it short-circuits since they
 * are translated far off-screen and can't receive touches.
 *
 * [headerOffsetState] is the *current* step's state — used so the header-top-padding modifier
 * stays in sync with the visible header during navigation, regardless of which step is being
 * laid out.
 */
@Suppress("LongParameterList")
@Composable
private fun WorkflowStepBody(
    stepId: String,
    stepState: PaywallState.Loaded.Components,
    isCurrent: Boolean,
    currentStepId: String,
    headerOffsetState: PaywallState.Loaded.Components,
    hasHeaderOverlay: Boolean,
    navigationDirection: NavigationDirection,
    slideState: WorkflowSlideState,
    clickHandler: suspend (PaywallAction.External) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
) {
    val onClick: suspend (PaywallAction) -> Unit = { action ->
        if (isCurrent) {
            handleClick(action, stepState, clickHandler, componentInteractionTracker)
        }
    }
    val tracker = if (isCurrent) componentInteractionTracker else PaywallComponentInteractionTracker { _ -> }

    ComponentView(
        style = stepState.stack,
        state = stepState,
        onClick = onClick,
        componentInteractionTracker = tracker,
        modifier = Modifier
            .fillMaxSize()
            .workflowSlide(slideState, stepId, currentStepId, navigationDirection)
            .verticalScroll(rememberScrollState())
            .conditional(hasHeaderOverlay && !stepState.mainStackHasHeroImage) {
                headerTopPadding(headerOffsetState)
            },
    )
}
