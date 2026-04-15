@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData

internal object WorkflowScreenMapper {

    fun toPaywallComponentsData(screen: WorkflowScreen): PaywallComponentsData =
        PaywallComponentsData(
            templateName = screen.templateName,
            assetBaseURL = screen.assetBaseURL,
            componentsConfig = screen.componentsConfig,
            componentsLocalizations = screen.componentsLocalizations,
            defaultLocaleIdentifier = screen.defaultLocaleIdentifier,
            revision = screen.revision,
        )

    fun toPaywallComponents(screen: WorkflowScreen, uiConfig: UiConfig): Offering.PaywallComponents =
        Offering.PaywallComponents(
            uiConfig = uiConfig,
            data = toPaywallComponentsData(screen),
        )
}
