@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)
@file:Suppress("MatchingDeclarationName")

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Box
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

internal fun headerAlpha(role: WorkflowHeaderTransitionRole, progress: Float): Float = when (role) {
    WorkflowHeaderTransitionRole.ENTERING -> progress
    WorkflowHeaderTransitionRole.LEAVING -> 1f - progress
    WorkflowHeaderTransitionRole.STABLE -> 1f
}

@Suppress("LongParameterList", "LongMethod")
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

    val headerPresentation = workflowHeaderState(
        currentStepId = currentStepId,
        stepStates = stepStates,
        transitionState = transitionState,
    )
    val headerState = stepStates[headerPresentation.headerStepId] ?: currentState
    val onClick: suspend (PaywallAction) -> Unit = { action ->
        handleClick(action, currentState, clickHandler, componentInteractionTracker)
    }
    val headerOnClick: suspend (PaywallAction) -> Unit = { action ->
        handleClick(action, headerState, clickHandler, componentInteractionTracker)
    }

    // When the header is LEAVING (fading out over an incoming step that has no header), routing it
    // through OverlayLayout would write its measured height into currentState.headerHeightPx.
    // The incoming step would then use that height as its hero ZLayer top inset instead of the
    // status-bar fallback. To avoid this, LEAVING headers are rendered as a Box overlay inside the
    // mainContent lambda — outside OverlayLayout — so currentState.headerHeightPx stays 0.
    val isLeavingHeader = headerPresentation.role == WorkflowHeaderTransitionRole.LEAVING
    val headerComposable: (@Composable () -> Unit)? = headerState.header?.let { headerStyle ->
        {
            ComponentView(
                style = headerStyle,
                state = headerState,
                onClick = headerOnClick,
                modifier = Modifier
                    .fillMaxWidth()
                    // Read animatable.value inside graphicsLayer (draw phase), like workflowTransition,
                    // so the fade stays in lock-step with the slide without recomposing every frame.
                    .graphicsLayer {
                        alpha = headerAlpha(headerPresentation.role, transitionState.animatable.value)
                    },
            )
        }
    }

    PaywallComponentsScaffold(
        state = currentState,
        modifier = modifier,
        background = null,
        headerContent = if (!isLeavingHeader) headerComposable else null,
    ) {
        if (isLeavingHeader && headerComposable != null) {
            // Box required to overlay the LEAVING header above WorkflowStepsContent.
            // Only present during a header→no-header transition; not added in the common case.
            Box(Modifier.fillMaxSize()) {
                WorkflowStepsContent(
                    currentStepId = currentStepId,
                    stepStates = stepStates,
                    transitionState = transitionState,
                    clickHandler = clickHandler,
                    componentInteractionTracker = componentInteractionTracker,
                )
                headerComposable()
            }
        } else {
            WorkflowStepsContent(
                currentStepId = currentStepId,
                stepStates = stepStates,
                transitionState = transitionState,
                clickHandler = clickHandler,
                componentInteractionTracker = componentInteractionTracker,
            )
        }
    }
}

private fun workflowHeaderState(
    currentStepId: String,
    stepStates: Map<String, PaywallState.Loaded.Components>,
    transitionState: WorkflowTransitionState,
): WorkflowHeaderPresentation {
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
    return selectWorkflowHeaderPresentation(
        currentStepId = currentStepId,
        stepInfoByStepId = headerStepInfo,
        pendingTransition = pendingTransition,
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
 * The header is rendered outside this surface (either via the scaffold's OverlayLayout or,
 * when LEAVING, as a Box overlay in the main content — see [LoadedWorkflowPaywall]).
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
            // The header for a workflow step is rendered by the scaffold, so hasHeader is false here.
            // A sticky footer, when present, overlays the bottom on top of the full-height content and
            // reserves clearance via footerBottomPadding (see PaywallComponentsScaffold).
            OverlayLayout(
                state = stepState,
                modifier = Modifier.fillMaxSize(),
                hasFooter = stepState.stickyFooter != null,
            ) {
                ComponentView(
                    style = stepState.stack,
                    state = stepState,
                    onClick = onClick,
                    componentInteractionTracker = tracker,
                    modifier = Modifier
                        .fillMaxSize()
                        .conditional(shouldWrapMainContentInVerticalScroll) {
                            verticalScroll(mainScrollState)
                        }
                        .conditional(stepState.header != null && !stepState.mainStackHasHeroImage) {
                            headerTopPadding(stepState)
                        }
                        .conditional(stepState.stickyFooter != null) {
                            footerBottomPadding(stepState)
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
        fromHasHeader && toHasHeader -> {
            // Both steps have a header, no fade
            // !! safe: fromHasHeader/toHasHeader imply their info is non-null
            val stepId = if (shouldUseOutgoingHeader(pendingTransition.direction, fromInfo!!, toInfo!!)) {
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
): Boolean = !toStepInfo.hasHeroImage &&
    (direction == NavigationDirection.BACKWARD || fromStepInfo.hasHeroImage)
