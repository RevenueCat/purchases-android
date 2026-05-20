@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components.inputsinglechoice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.workflows.WorkflowTriggerType
import com.revenuecat.purchases.paywalls.events.PaywallComponentInteractionData
import com.revenuecat.purchases.paywalls.events.PaywallComponentType
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.InputOptionComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import kotlinx.coroutines.launch

@Composable
internal fun InputOptionComponentView(
    style: InputOptionComponentStyle,
    state: PaywallState.Loaded.Components,
    onClick: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
    componentInteractionTracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
) {
    val coroutineScope = rememberCoroutineScope()
    val isSelected by remember {
        derivedStateOf { state.selectedOptionIdsByFieldId[style.fieldId] == style.optionId }
    }

    StackComponentView(
        style = style.stack,
        state = state,
        clickHandler = { },
        componentInteractionTracker = componentInteractionTracker,
        modifier = modifier,
        enabled = !isSelected,
        onStackClick = {
            if (isSelected) return@StackComponentView
            state.update(fieldId = style.fieldId, selectedOptionId = style.optionId)
            componentInteractionTracker.track(
                PaywallComponentInteractionData(
                    componentType = PaywallComponentType.INPUT_OPTION,
                    componentValue = style.optionId,
                ),
            )
            coroutineScope.launch {
                onClick(PaywallAction.External.WorkflowTrigger(style.optionId, WorkflowTriggerType.ON_PRESS))
            }
        },
    )
}
