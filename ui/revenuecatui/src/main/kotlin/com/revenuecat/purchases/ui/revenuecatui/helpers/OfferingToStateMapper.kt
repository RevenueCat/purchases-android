package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewMode
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfigurationFactory
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider

@Suppress("ReturnCount", "TooGenericExceptionCaught")
internal fun Offering.toPaywallViewState(
    variableDataProvider: VariableDataProvider,
    mode: PaywallViewMode,
): PaywallViewState {
    val paywallData = this.paywall
        ?: return PaywallViewState.Error("No paywall data for offering: $identifier")
    val templateConfiguration = TemplateConfigurationFactory.create(
        variableDataProvider = variableDataProvider,
        mode = mode,
        paywallData = paywallData,
        packages = availablePackages,
        activelySubscribedProductIdentifiers = emptySet(), // TODO-PAYWALLS: Check for active subscriptions
    )
    PaywallViewState.Loaded(
        templateConfiguration = templateConfiguration,
        selectedPackage = templateConfiguration.packages.default,
    )
}
