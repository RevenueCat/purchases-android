package com.revenuecat.purchases.ui.revenuecatui.errors

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.strings.PaywallValidationErrorStrings

sealed class PaywallValidationError : Throwable() {

    abstract fun associatedErrorString(offering: Offering): String

    object MissingPaywall : PaywallValidationError() {
        override fun associatedErrorString(offering: Offering): String {
            return PaywallValidationErrorStrings.MISSING_PAYWALL.format(offering.identifier)
        }
    }

    data class InvalidTemplate(val templateName: String) : PaywallValidationError() {
        override fun associatedErrorString(offering: Offering): String {
            return PaywallValidationErrorStrings.INVALID_TEMPLATE_NAME.format(templateName)
        }
    }

    data class InvalidVariables(val unrecognizedVariables: Set<String>) : PaywallValidationError() {
        override fun associatedErrorString(offering: Offering): String {
            val joinedUnrecognizedVariables = this.unrecognizedVariables.joinToString()
            return PaywallValidationErrorStrings.INVALID_VARIABLES.format(joinedUnrecognizedVariables)
        }
    }

    data class InvalidIcons(val invalidIcons: Set<String>) : PaywallValidationError() {
        override fun associatedErrorString(offering: Offering): String {
            val joinedInvalidIcons = this.invalidIcons.joinToString()
            return PaywallValidationErrorStrings.INVALID_ICONS.format(joinedInvalidIcons)
        }
    }
}
