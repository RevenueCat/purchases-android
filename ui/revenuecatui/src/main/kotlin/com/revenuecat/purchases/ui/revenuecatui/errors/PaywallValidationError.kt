package com.revenuecat.purchases.ui.revenuecatui.errors

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.strings.PaywallValidationErrorStrings

internal sealed class PaywallValidationError : Throwable() {

    fun associatedErrorString(offering: Offering): String {
        return when (this) {
            is InvalidIcons -> {
                val joinedInvalidIcons = this.invalidIcons.joinToString()
                PaywallValidationErrorStrings.INVALID_ICONS.format(joinedInvalidIcons)
            }
            is InvalidTemplate -> PaywallValidationErrorStrings.INVALID_TEMPLATE_NAME.format(templateName)
            is InvalidVariables -> {
                val joinedUnrecognizedVariables = this.unrecognizedVariables.joinToString()
                PaywallValidationErrorStrings.INVALID_VARIABLES.format(joinedUnrecognizedVariables)
            }
            MissingPaywall -> PaywallValidationErrorStrings.MISSING_PAYWALL.format(offering.identifier)
        }
    }

    object MissingPaywall : PaywallValidationError()
    data class InvalidTemplate(val templateName: String) : PaywallValidationError()
    data class InvalidVariables(val unrecognizedVariables: Set<String>) : PaywallValidationError()
    data class InvalidIcons(val invalidIcons: Set<String>) : PaywallValidationError()
}
