@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.WorkflowPaywallUiState
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker

@Composable
internal fun LoadedWorkflowPaywall(
    workflowState: WorkflowPaywallUiState,
    clickHandler: suspend (PaywallAction.External) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
    modifier: Modifier = Modifier,
) {
    val currentStepId = workflowState.currentStepId
    val currentState = workflowState.stepStates[currentStepId] ?: run {
        Logger.e("Workflow step '$currentStepId' not found in stepStates — rendering nothing")
        return
    }

    val configuration = LocalConfiguration.current
    currentState.update(localeList = configuration.locales)

    val onClick: suspend (PaywallAction) -> Unit = { action: PaywallAction ->
        handleClick(action, currentState, clickHandler, componentInteractionTracker)
    }

    val shouldWrapMainContentInVerticalScroll =
        (currentState.stack as? StackComponentStyle)?.scrollOrientation != Orientation.Vertical
    val mainScrollState = rememberScrollState()

    PaywallComponentsScaffold(
        state = currentState,
        clickHandler = clickHandler,
        componentInteractionTracker = componentInteractionTracker,
        modifier = modifier,
    ) {
        ComponentView(
            style = currentState.stack,
            state = currentState,
            onClick = onClick,
            componentInteractionTracker = componentInteractionTracker,
            modifier = Modifier
                .fillMaxSize()
                .conditional(shouldWrapMainContentInVerticalScroll) {
                    verticalScroll(mainScrollState)
                }
                .conditional(currentState.header != null && !currentState.mainStackHasHeroImage) {
                    headerTopPadding(currentState)
                },
        )
    }
}
