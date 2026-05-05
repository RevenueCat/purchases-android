@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)
@file:Suppress("MatchingDeclarationName")

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
import com.revenuecat.purchases.ui.revenuecatui.data.WorkflowPendingTransition
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.workflow.NavigationDirection

internal data class WorkflowHeaderStepInfo(
    val hasHeroImage: Boolean,
    val hasHeader: Boolean,
)

@Composable
internal fun LoadedWorkflowPaywall(
    workflowState: WorkflowPaywallUiState,
    onTransitionComplete: (transitionId: Int) -> Unit,
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
        onTransitionComplete = onTransitionComplete,
    )

    val headerState = workflowHeaderState(
        currentStepId = currentStepId,
        currentState = currentState,
        stepStates = stepStates,
        slideState = slideState,
    )
    val onClick: suspend (PaywallAction) -> Unit = { action ->
        handleClick(action, currentState, clickHandler, componentInteractionTracker)
    }
    PaywallComponentsScaffold(
        state = currentState,
        modifier = modifier,
        background = null,
        headerContent = headerState.header?.let { headerStyle ->
            {
                ComponentView(
                    style = headerStyle,
                    state = headerState,
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        WorkflowStepsContent(
            currentStepId = currentStepId,
            stepStates = stepStates,
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
        pendingTransition = if (slideState.animatingFromStepId != null && slideState.animatingDirection != null) {
            WorkflowPendingTransition(
                fromStepId = slideState.animatingFromStepId,
                direction = slideState.animatingDirection,
                id = 0, // id not needed for header selection
            )
        } else {
            null
        },
    )

    return stepStates[headerStepId] ?: currentState
}

@Suppress("LongParameterList")
@Composable
private fun WorkflowStepsContent(
    currentStepId: String,
    stepStates: Map<String, PaywallState.Loaded.Components>,
    slideState: WorkflowSlideState,
    clickHandler: suspend (PaywallAction.External) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
) {
    // Multi-step slide container: the current and outgoing steps are stacked and translated by workflowSlide.
    // No clipToBounds here — horizontal overflow is bounded by the window/dialog, and adding
    // a top clip causes the hero image (which renders behind the status bar) to get cropped
    // during the slide transition.
    Box(modifier = Modifier.fillMaxSize()) {
        for (stepId in slideState.visibleStepIds) {
            val stepState = stepStates[stepId] ?: continue
            key(stepId) {
                WorkflowStepContent(
                    stepId = stepId,
                    stepState = stepState,
                    currentStepId = currentStepId,
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
    currentStepId: String,
    slideState: WorkflowSlideState,
    clickHandler: suspend (PaywallAction.External) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
) {
    val isCurrent = stepId == currentStepId
    val onClick: suspend (PaywallAction) -> Unit = { action ->
        if (isCurrent) {
            handleClick(action, stepState, clickHandler, componentInteractionTracker)
        }
    }
    val tracker = if (isCurrent) componentInteractionTracker else PaywallComponentInteractionTracker { _ -> }
    val background = rememberBackgroundStyle(stepState.background)
    val shouldWrapMainContentInVerticalScroll = shouldWrapMainContentInVerticalScroll(stepState.stack)
    val mainScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .workflowSlide(slideState, stepId, currentStepId)
            .background(background),
    ) {
        WithOptionalBackgroundOverlay(
            state = stepState,
            background = background,
            modifier = Modifier.fillMaxSize(),
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
                        .conditional(shouldWrapMainContentInVerticalScroll) {
                            verticalScroll(mainScrollState)
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
}

internal fun selectWorkflowHeaderStepId(
    currentStepId: String,
    stepInfoByStepId: Map<String, WorkflowHeaderStepInfo>,
    pendingTransition: WorkflowPendingTransition?,
): String {
    val fromStepId = pendingTransition?.fromStepId
    val direction = pendingTransition?.direction
    val fromStepInfo = fromStepId?.let(stepInfoByStepId::get)
    val toStepInfo = stepInfoByStepId[currentStepId]
    val useOutgoingHeader = pendingTransition != null &&
        fromStepInfo != null &&
        toStepInfo != null &&
        shouldUseOutgoingHeader(direction, fromStepInfo, toStepInfo)

    return if (useOutgoingHeader) fromStepId!! else currentStepId
}

private fun shouldUseOutgoingHeader(
    direction: NavigationDirection?,
    fromStepInfo: WorkflowHeaderStepInfo,
    toStepInfo: WorkflowHeaderStepInfo,
): Boolean = fromStepInfo.hasHeader &&
    !toStepInfo.hasHeroImage &&
    (direction == NavigationDirection.BACKWARD || fromStepInfo.hasHeroImage)
