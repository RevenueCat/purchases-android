package com.revenuecat.purchases.ui.revenuecatui.strings

internal object PaywallValidationErrorStrings {
    const val DISPLAYING_DEFAULT = "Displaying default template due to validation errors."
    const val MISSING_PAYWALL = "Displaying default template because paywall is missing for offering '%s'."
    const val INVALID_VARIABLES = "There were some errors validating variables in the paywall strings. " +
        "The unrecognized variables are: %s"
    const val INVALID_TEMPLATE_NAME = "Template name is not recognized: %s"
    const val INVALID_ICONS = "One or more icons were not recognized: %s"
    const val MISSING_TIERS = "Displaying default template because paywall tiers are missing for offering '%s'."
    const val MISSING_TIER_CONFIGURATIONS = "There are required tier configurations missing for: '%s'."
    const val MISSING_STRING_LOCALIZATION = "Missing string localization for property with id: '%s'"
    const val MISSING_IMAGE_LOCALIZATION = "Missing image localization for property with id: '%s'"
    const val MISSING_STRING_LOCALIZATION_WITH_LOCALE =
        "Missing string localization for property with id: '%s', for locale: '%s'."
    const val MISSING_IMAGE_LOCALIZATION_WITH_LOCALE =
        "Missing image localization for property with id: '%s', for locale: '%s'."
    const val ALL_LOCALIZATIONS_MISSING_FOR_LOCALE =
        "All localizations for locale '%s' are missing."
}
