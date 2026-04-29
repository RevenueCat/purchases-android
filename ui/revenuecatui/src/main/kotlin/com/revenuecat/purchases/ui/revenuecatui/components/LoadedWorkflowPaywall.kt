@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components

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

private data class ResolvedWorkflowHeaderTransition(
    val fromStepId: String?,
    val direction: NavigationDirection,
    val inGap: Boolean,
) {
    val isAnimating: Boolean
        get() = fromStepId != null && direction != NavigationDirection.NONE
}

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
    val composedStatesForHeaderHeight = workflowStatesForHeaderHeight(
        currentStepId = currentStepId,
        stepStates = stepStates,
        slideState = slideState,
    )

    PaywallComponentsScaffold(
        state = currentState,
        headerState = headerState,
        backgroundState = null,
        composedStatesForHeaderHeight = composedStatesForHeaderHeight,
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
    val headerStepId = slideState.pinnedHeaderStepId ?: selectWorkflowHeaderStepId(
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
                    .verticalScroll(rememberScrollState())
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

internal fun selectWorkflowHeaderStepId(
    currentStepId: String,
    stepInfoByStepId: Map<String, WorkflowHeaderStepInfo>,
    transitionState: WorkflowHeaderTransitionState,
): String {
    val transition = transitionState.resolve(currentStepId)
    val fromStepInfo = transition.fromStepId?.let(stepInfoByStepId::get)
    val toStepInfo = stepInfoByStepId[currentStepId]
    val useOutgoingHeader = transition.isAnimating &&
        fromStepInfo != null &&
        toStepInfo != null &&
        shouldUseOutgoingHeader(transition, fromStepInfo, toStepInfo)

    return if (useOutgoingHeader) {
        transition.fromStepId ?: currentStepId
    } else {
        currentStepId
    }
}

private fun WorkflowHeaderTransitionState.resolve(currentStepId: String): ResolvedWorkflowHeaderTransition {
    val inGap = seenStepId != currentStepId
    return ResolvedWorkflowHeaderTransition(
        fromStepId = if (inGap) seenStepId else animatingFromStepId,
        direction = if (inGap) navigationDirection else animatingDirection,
        inGap = inGap,
    )
}

private fun shouldUseOutgoingHeader(
    transition: ResolvedWorkflowHeaderTransition,
    fromStepInfo: WorkflowHeaderStepInfo,
    toStepInfo: WorkflowHeaderStepInfo,
): Boolean =
    if (transition.inGap) {
        fromStepInfo.hasHeader
    } else {
        fromStepInfo.hasHeader &&
            !toStepInfo.hasHeroImage &&
            (transition.direction == NavigationDirection.BACKWARD || fromStepInfo.hasHeroImage)
    }
