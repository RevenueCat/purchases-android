@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData

internal object WorkflowScreenMapper {

    fun toPaywallComponentsData(screen: WorkflowScreen, screenId: String): PaywallComponentsData =
        PaywallComponentsData(
            id = screenId,
            templateName = screen.templateName,
            assetBaseURL = screen.assetBaseURL,
            componentsConfig = screen.componentsConfig,
            componentsLocalizations = screen.componentsLocalizations,
            defaultLocaleIdentifier = screen.defaultLocaleIdentifier,
            revision = screen.revision,
            exitOffers = screen.exitOffers,
        )

    fun toPaywallComponents(screen: WorkflowScreen, screenId: String, uiConfig: UiConfig): Offering.PaywallComponents =
        Offering.PaywallComponents(
            uiConfig = uiConfig,
            data = toPaywallComponentsData(screen, screenId),
        )
}
