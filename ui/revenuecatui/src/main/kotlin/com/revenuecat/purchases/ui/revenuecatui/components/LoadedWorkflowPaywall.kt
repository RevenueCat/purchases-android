@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.WorkflowPaywallUiState
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.workflow.NavigationDirection

internal data class WorkflowHeaderTransitionState(
    val seenStepId: String,
    val animatingFromStepId: String?,
    val animatingDirection: NavigationDirection,
    val navigationDirection: NavigationDirection,
)

internal data class WorkflowHeaderStepInfo(
    val hasHeroImage: Boolean,
    val hasHeader: Boolean,
)

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

    val headerState = workflowHeaderState(
        currentStepId = currentStepId,
        currentState = currentState,
        stepStates = stepStates,
        navigationDirection = navigationDirection,
        slideState = slideState,
    )
    val statesForHeaderHeight = workflowStatesForHeaderHeight(
        currentStepId = currentStepId,
        stepStates = stepStates,
        slideState = slideState,
    )

    PaywallComponentsScaffold(
        state = currentState,
        headerState = headerState,
        backgroundState = null,
        statesForHeaderHeight = statesForHeaderHeight,
        renderStickyFooter = false,
        clickHandler = clickHandler,
        componentInteractionTracker = componentInteractionTracker,
        modifier = modifier,
    ) {
        WorkflowStepsContent(
            currentStepId = currentStepId,
            stepStates = stepStates,
            navigationDirection = navigationDirection,
            slideState = slideState,
            clickHandler = clickHandler,
            componentInteractionTracker = componentInteractionTracker,
        )
    }
}

private fun workflowHeaderState(
    currentStepId: String,
    currentState: PaywallState.Loaded.Components,
    stepStates: Map<String, PaywallState.Loaded.Components>,
    navigationDirection: NavigationDirection,
    slideState: WorkflowSlideState,
): PaywallState.Loaded.Components {
    val headerStepInfo = stepStates.mapValues { (_, stepState) ->
        WorkflowHeaderStepInfo(
            hasHeroImage = stepState.mainStackHasHeroImage,
            hasHeader = stepState.header != null,
        )
    }
    val headerStepId = selectWorkflowHeaderStepId(
        currentStepId = currentStepId,
        stepInfoByStepId = headerStepInfo,
        transitionState = WorkflowHeaderTransitionState(
            seenStepId = slideState.seenStepId,
            animatingFromStepId = slideState.animatingFromStepId,
            animatingDirection = slideState.animatingDirection,
            navigationDirection = navigationDirection,
        ),
    )

    return stepStates[headerStepId] ?: currentState
}

private fun workflowStatesForHeaderHeight(
    currentStepId: String,
    stepStates: Map<String, PaywallState.Loaded.Components>,
    slideState: WorkflowSlideState,
): List<PaywallState.Loaded.Components> {
    val statesForHeaderHeightIds = buildSet {
        addAll(slideState.composedStepIds)
        add(currentStepId)
        add(slideState.seenStepId)
        slideState.animatingFromStepId?.let(::add)
    }

    return statesForHeaderHeightIds.mapNotNull(stepStates::get)
}

@Suppress("LongParameterList")
@Composable
private fun WorkflowStepsContent(
    currentStepId: String,
    stepStates: Map<String, PaywallState.Loaded.Components>,
    navigationDirection: NavigationDirection,
    slideState: WorkflowSlideState,
    clickHandler: suspend (PaywallAction.External) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
) {
    // Multi-step slide container: all composed steps are stacked and translated by workflowSlide.
    // No clipToBounds here — horizontal overflow is bounded by the window/dialog, and adding
    // a top clip causes the hero image (which renders behind the status bar) to get cropped
    // during the slide transition.
    Box(modifier = Modifier.fillMaxSize()) {
        for (stepId in slideState.composedStepIds) {
            val stepState = stepStates[stepId] ?: continue
            key(stepId) {
                WorkflowStepContent(
                    stepId = stepId,
                    stepState = stepState,
                    isCurrent = stepId == currentStepId,
                    currentStepId = currentStepId,
                    navigationDirection = navigationDirection,
                    slideState = slideState,
                    clickHandler = clickHandler,
                    componentInteractionTracker = componentInteractionTracker,
                )
            }
        }
    }
}

/**
 * Renders one workflow step's body and footer as a self-contained sliding surface.
 * Header is rendered at scaffold level and stays fixed.
 * Off-screen (parked) steps still receive a click handler, but it short-circuits because they are
 * translated off-screen and can't receive touches.
 */
@Suppress("LongParameterList")
@Composable
private fun WorkflowStepContent(
    stepId: String,
    stepState: PaywallState.Loaded.Components,
    isCurrent: Boolean,
    currentStepId: String,
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
    val background = rememberBackgroundStyle(stepState.background)
    // Skip the outer verticalScroll if the step's root stack already scrolls vertically — see
    // LoadedPaywallComponents for the rationale.
    val shouldWrapInVerticalScroll =
        (stepState.stack as? StackComponentStyle)?.scrollOrientation != Orientation.Vertical
    val scrollState = rememberScrollState()

    WithOptionalBackgroundOverlay(
        state = stepState,
        background = background,
        modifier = Modifier
            .fillMaxSize()
            .workflowSlide(slideState, stepId, currentStepId, navigationDirection)
            .background(background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ComponentView(
                style = stepState.stack,
                state = stepState,
                onClick = onClick,
                componentInteractionTracker = tracker,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .conditional(shouldWrapInVerticalScroll) {
                        verticalScroll(scrollState)
                    }
                    .conditional(stepState.header != null && !stepState.mainStackHasHeroImage) {
                        headerTopPadding(stepState)
                    },
            )
            stepState.stickyFooter?.let { footerStyle ->
                ComponentView(
                    style = footerStyle,
                    state = stepState,
                    onClick = onClick,
                    componentInteractionTracker = tracker,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Picks which step's header should be rendered at the scaffold level during a workflow transition.
 *
 * The visual goal: avoid header "flashes" mid-animation. Two cases need the outgoing step's header
 * to stay visible while the body slides:
 *  - **Hero → non-hero (any direction):** the outgoing hero header would otherwise pop off before
 *    the body finishes sliding, briefly exposing the new step's plain background under the old hero.
 *  - **Backward navigation with an outgoing header:** users expect the previous screen's header to
 *    remain "the surface they're returning toward", so we keep it until the slide completes.
 *
 * Otherwise we use the incoming step's header (or none).
 *
 * [currentStepId] is the step we're navigating to. The "from" step is taken from
 * [WorkflowHeaderTransitionState.animatingFromStepId] once the animation has bound, or from
 * [WorkflowHeaderTransitionState.seenStepId] during the brief gap between recomposition and the
 * `LaunchedEffect` running.
 */
internal fun selectWorkflowHeaderStepId(
    currentStepId: String,
    stepInfoByStepId: Map<String, WorkflowHeaderStepInfo>,
    transitionState: WorkflowHeaderTransitionState,
): String {
    val inGap = transitionState.seenStepId != currentStepId
    val fromStepId = if (inGap) transitionState.seenStepId else transitionState.animatingFromStepId
    val direction = if (inGap) transitionState.navigationDirection else transitionState.animatingDirection
    val fromStepInfo = fromStepId?.let { stepInfoByStepId[it] }
    val toStepInfo = stepInfoByStepId[currentStepId]

    val outgoing = fromStepId.takeIf {
        fromStepInfo != null &&
            toStepInfo != null &&
            direction != NavigationDirection.NONE &&
            shouldKeepOutgoingHeader(inGap, direction, fromStepInfo, toStepInfo)
    }
    return outgoing ?: currentStepId
}

private fun shouldKeepOutgoingHeader(
    inGap: Boolean,
    direction: NavigationDirection,
    fromStepInfo: WorkflowHeaderStepInfo,
    toStepInfo: WorkflowHeaderStepInfo,
): Boolean = if (inGap) {
    fromStepInfo.hasHeader
} else {
    fromStepInfo.hasHeader &&
        !toStepInfo.hasHeroImage &&
        (direction == NavigationDirection.BACKWARD || fromStepInfo.hasHeroImage)
}
