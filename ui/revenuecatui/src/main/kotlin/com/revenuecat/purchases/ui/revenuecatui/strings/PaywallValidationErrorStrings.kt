package com.revenuecat.purchases.ui.revenuecatui.strings

internal object PaywallValidationErrorStrings {
    const val DISPLAYING_DEFAULT = "Displaying default template due to validation errors."
    const val MISSING_PAYWALL = "Displaying default template because paywall is missing for offering '%s'."
    const val INVALID_VARIABLES = "There were some errors validating variables in the paywall strings. " +
        "The unrecognized variables are: %s"
    const val INVALID_TEMPLATE_NAME = "Template name is not recognized: %s"
    const val INVALID_ICONS = "One or more icons were not recognized: %s"
}
