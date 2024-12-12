package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError

internal sealed interface PaywallValidationResult {
    val error: PaywallValidationError?

    data class Legacy(
        val displayablePaywall: PaywallData,
        val template: PaywallTemplate,
        override val error: PaywallValidationError? = null,
    ) : PaywallValidationResult

    data class Components(
        val displayablePaywall: PaywallComponentsData,
        override val error: PaywallValidationError? = null,
    ) : PaywallValidationResult
}
