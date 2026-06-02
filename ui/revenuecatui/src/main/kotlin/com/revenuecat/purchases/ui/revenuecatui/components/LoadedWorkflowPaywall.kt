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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
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

internal enum class WorkflowHeaderTransitionRole { ENTERING, LEAVING, STABLE }

internal data class WorkflowHeaderPresentation(
    val headerStepId: String,
    val role: WorkflowHeaderTransitionRole,
)

internal data class WorkflowHeaderRender(
    val state: PaywallState.Loaded.Components,
    val role: WorkflowHeaderTransitionRole,
)

internal fun headerAlpha(role: WorkflowHeaderTransitionRole, progress: Float): Float = when (role) {
    WorkflowHeaderTransitionRole.ENTERING -> progress
    WorkflowHeaderTransitionRole.LEAVING -> 1f - progress
    WorkflowHeaderTransitionRole.STABLE -> 1f
}

@Suppress("LongParameterList")
@Composable
internal fun LoadedWorkflowPaywall(
    workflowState: WorkflowPaywallUiState,
    onTransitionComplete: (transitionId: Int) -> Unit,
    clickHandler: suspend (PaywallAction.External) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
    modifier: Modifier = Modifier,
    transition: WorkflowTransitionAnimation = WorkflowTransitionAnimation.SlideInOut(),
) {
    val currentStepId = workflowState.currentStepId
    val stepStates = workflowState.stepStates
    val currentState = stepStates[currentStepId] ?: run {
        Logger.e("Workflow step '$currentStepId' not found in stepStates — rendering nothing")
        return
    }

    val configuration = LocalConfiguration.current
    currentState.update(localeList = configuration.locales)

    val transitionState = rememberWorkflowTransitionState(
        workflowState = workflowState,
        onTransitionComplete = onTransitionComplete,
        transition = transition,
    )

    val headerRender = workflowHeaderState(
        currentStepId = currentStepId,
        currentState = currentState,
        stepStates = stepStates,
        transitionState = transitionState,
    )
    val onClick: suspend (PaywallAction) -> Unit = { action ->
        handleClick(action, currentState, clickHandler, componentInteractionTracker)
    }
    PaywallComponentsScaffold(
        state = currentState,
        modifier = modifier,
        background = null,
        headerContent = headerRender.state.header?.let { headerStyle ->
            {
                ComponentView(
                    style = headerStyle,
                    state = headerRender.state,
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        // Read animatable.value inside graphicsLayer (draw phase), like workflowTransition,
                        // so the fade stays in lock-step with the slide without recomposing every frame.
                        .graphicsLayer { alpha = headerAlpha(headerRender.role, transitionState.animatable.value) },
                )
            }
        },
    ) {
        WorkflowStepsContent(
            currentStepId = currentStepId,
            stepStates = stepStates,
            transitionState = transitionState,
            clickHandler = clickHandler,
            componentInteractionTracker = componentInteractionTracker,
        )
    }
}

private fun workflowHeaderState(
    currentStepId: String,
    currentState: PaywallState.Loaded.Components,
    stepStates: Map<String, PaywallState.Loaded.Components>,
    transitionState: WorkflowTransitionState,
): WorkflowHeaderRender {
    val headerStepInfo = stepStates.mapValues { (_, stepState) ->
        WorkflowHeaderStepInfo(
            hasHeroImage = stepState.mainStackHasHeroImage,
            hasHeader = stepState.header != null,
        )
    }
    val pendingTransition = when (transitionState) {
        is WorkflowTransitionState.SlideInOut -> transitionState.animatingFromStepId?.let { fromStepId ->
            transitionState.animatingDirection?.let { direction ->
                WorkflowPendingTransition(
                    fromStepId = fromStepId,
                    direction = direction,
                    id = 0, // id not needed for header selection
                )
            }
        }
    }
    val presentation = selectWorkflowHeaderPresentation(
        currentStepId = currentStepId,
        stepInfoByStepId = headerStepInfo,
        pendingTransition = pendingTransition,
    )

    return WorkflowHeaderRender(
        state = stepStates[presentation.headerStepId] ?: currentState,
        role = presentation.role,
    )
}

@Suppress("LongParameterList")
@Composable
private fun WorkflowStepsContent(
    currentStepId: String,
    stepStates: Map<String, PaywallState.Loaded.Components>,
    transitionState: WorkflowTransitionState,
    clickHandler: suspend (PaywallAction.External) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
) {
    // Multi-step container: the current and outgoing steps are stacked and translated by workflowTransition.
    // No clipToBounds here — horizontal overflow is bounded by the window/dialog, and adding
    // a top clip causes the hero image (which renders behind the status bar) to get cropped
    // during the slide transition.
    Box(modifier = Modifier.fillMaxSize()) {
        listOfNotNull(transitionState.animatingFromStepId, transitionState.animatingToStepId)
            .forEach { stepId ->
                val stepState = stepStates[stepId] ?: return@forEach
                key(stepId) {
                    WorkflowStepContent(
                        stepId = stepId,
                        stepState = stepState,
                        currentStepId = currentStepId,
                        transitionState = transitionState,
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
    transitionState: WorkflowTransitionState,
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
    val layoutDirection = LocalLayoutDirection.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .workflowTransition(transitionState, stepId, layoutDirection)
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

internal fun selectWorkflowHeaderPresentation(
    currentStepId: String,
    stepInfoByStepId: Map<String, WorkflowHeaderStepInfo>,
    pendingTransition: WorkflowPendingTransition?,
): WorkflowHeaderPresentation {
    val fromStepId = pendingTransition?.fromStepId
        ?: return WorkflowHeaderPresentation(currentStepId, WorkflowHeaderTransitionRole.STABLE)
    val fromInfo = stepInfoByStepId[fromStepId]
    val toInfo = stepInfoByStepId[currentStepId]
    val fromHasHeader = fromInfo?.hasHeader == true
    val toHasHeader = toInfo?.hasHeader == true

    return when {
        fromHasHeader && !toHasHeader ->
            WorkflowHeaderPresentation(fromStepId, WorkflowHeaderTransitionRole.LEAVING)
        !fromHasHeader && toHasHeader ->
            WorkflowHeaderPresentation(currentStepId, WorkflowHeaderTransitionRole.ENTERING)
        fromInfo != null && toInfo != null && fromHasHeader && toHasHeader -> {
            // Both steps have a header: no fade, keep the existing selection. Animating a change
            // between two different headers is a separate, deferred decision.
            val stepId = if (shouldUseOutgoingHeader(pendingTransition.direction, fromInfo, toInfo)) {
                fromStepId
            } else {
                currentStepId
            }
            WorkflowHeaderPresentation(stepId, WorkflowHeaderTransitionRole.STABLE)
        }
        else -> WorkflowHeaderPresentation(currentStepId, WorkflowHeaderTransitionRole.STABLE)
    }
}

private fun shouldUseOutgoingHeader(
    direction: NavigationDirection?,
    fromStepInfo: WorkflowHeaderStepInfo,
    toStepInfo: WorkflowHeaderStepInfo,
): Boolean = fromStepInfo.hasHeader &&
    !toStepInfo.hasHeroImage &&
    (direction == NavigationDirection.BACKWARD || fromStepInfo.hasHeroImage)
