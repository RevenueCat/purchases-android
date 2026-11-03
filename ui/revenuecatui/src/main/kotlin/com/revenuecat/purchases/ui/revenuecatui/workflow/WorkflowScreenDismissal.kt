package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.utils.filter

/**
 * Whether the screen has a button that leaves the flow from its first step: a close button, or a back button,
 * which has no earlier step to go to there. A button is counted whatever its visibility, so a close button that
 * only some conditions show still lets back dismiss.
 */
@OptIn(InternalRevenueCatAPI::class)
internal fun WorkflowScreen.hasDismissButton(): Boolean {
    val config = componentsConfig.base
    return listOfNotNull(config.stack, config.header, config.stickyFooter).any { root ->
        root.filter(::isDismissButton).isNotEmpty()
    }
}

@OptIn(InternalRevenueCatAPI::class)
private fun isDismissButton(component: PaywallComponent): Boolean {
    val action = (component as? ButtonComponent)?.action
    return action is ButtonComponent.Action.CloseWorkflow || action is ButtonComponent.Action.NavigateBack
}
