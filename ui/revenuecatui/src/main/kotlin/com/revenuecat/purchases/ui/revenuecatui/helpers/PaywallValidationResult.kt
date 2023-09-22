package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError

internal data class PaywallValidationResult(
    val displayablePaywall: PaywallData,
    val template: PaywallTemplate,
    val error: PaywallValidationError?,
)
