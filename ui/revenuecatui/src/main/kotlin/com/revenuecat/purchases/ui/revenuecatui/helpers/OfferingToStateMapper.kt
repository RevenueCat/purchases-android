package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewMode
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfigurationFactory
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import java.util.Locale

@Suppress("ReturnCount", "TooGenericExceptionCaught")
internal fun Offering.toPaywallViewState(variableDataProvider: VariableDataProvider): PaywallViewState {
    val paywallData = this.paywall
        ?: return PaywallViewState.Error("No paywall data for offering: $identifier")
    return try {
        val templateConfiguration = TemplateConfigurationFactory.create(
            variableDataProvider = variableDataProvider,
            mode = PaywallViewMode.FULL_SCREEN,
            paywallData = paywallData,
            packages = availablePackages,
            activelySubscribedProductIdentifiers = emptySet(), // TODO-PAYWALLS: Check for active subscriptions
            locale = Locale.getDefault(), // TODO-PAYWALLS: Use the correct locale
        )
        PaywallViewState.Loaded(
            templateConfiguration = templateConfiguration,
            selectedPackage = templateConfiguration.packages.default,
        )
    } catch (e: Exception) {
        PaywallViewState.Error("Error creating paywall: ${e.message}")
    }
}
