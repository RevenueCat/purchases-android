package com.revenuecat.purchases.ui.revenuecatui.strings

internal object PaywallValidationErrorStrings {
    const val DISPLAYING_DEFAULT = "Displaying default template due to validation errors."
    const val MISSING_PAYWALL = "Displaying default template because paywall is missing for offering '%s'. If you " +
        "expected to see a v2 Paywall, make sure it is published."
    const val INVALID_VARIABLES = "There were some errors validating variables in the paywall strings. " +
        "The unrecognized variables are: %s"
    const val INVALID_TEMPLATE_NAME = "Template name is not recognized: %s"
    const val INVALID_ICONS = "One or more icons were not recognized: %s"
    const val MISSING_TIERS = "Displaying default template because paywall tiers are missing for offering '%s'."
    const val MISSING_TIER_CONFIGURATIONS = "There are required tier configurations missing for: '%s'."
    const val MISSING_STRING_LOCALIZATION = "Missing string localization for property with id: '%s'"
    const val MISSING_IMAGE_LOCALIZATION = "Missing image localization for property with id: '%s'"
    const val MISSING_VIDEO_LOCALIZATION = "Missing video localization for property with id: '%s'"
    const val MISSING_STRING_LOCALIZATION_WITH_LOCALE =
        "Missing string localization for property with id: '%s', for locale: '%s'."
    const val MISSING_IMAGE_LOCALIZATION_WITH_LOCALE =
        "Missing image localization for property with id: '%s', for locale: '%s'."
    const val MISSING_VIDEO_LOCALIZATION_WITH_LOCALE =
        "Missing video localization for property with id: '%s', for locale: '%s'."
    const val ALL_LOCALIZATIONS_MISSING_FOR_LOCALE =
        "All localizations for locale '%s' are missing."
    const val ALL_VARIABLE_LOCALIZATIONS_MISSING_FOR_LOCALE =
        "All variable localizations for locale '%s' are missing."
    const val MISSING_PACKAGE =
        "The Paywall references a package with id '%s', but Offering '%s' does not contain such a package. " +
            "It has these packages instead: [%s]. Either add the missing package to the Offering or remove it from " +
            "the Paywall."
    const val MISSING_COLOR_ALIAS = "Aliased color '%s' does not exist."
    const val ALIASED_COLOR_IS_ALIAS = "Aliased color '%s' has an aliased value '%s', which is not allowed."
    const val MISSING_FONT_ALIAS = "Aliased font '%s' does not exist."
    const val INVALID_MODE_FOR_COMPONENTS_PAYWALL =
        "Paywalls V2 does not support footer modes. Falling back to legacy fallback paywall."
    const val TABS_COMPONENT_WITHOUT_TABS = "Tabs component has no tabs configured."
    const val TAB_CONTROL_NOT_IN_TAB = "Encountered a Tab Control component that is not in any tab."
    const val UNSUPPORTED_BACKGROUND_TYPE = "This SDK version does not support this background type: %s"
    const val ROOT_COMPONENT_UNSUPPORTED_PROPERTIES =
        "This paywall's root component is hidden because it contains unsupported properties: %s"
    const val UNSUPPORTED_CONDITION =
        "This paywall contains component overrides with conditions that are not supported by this SDK version. " +
            "Falling back to default paywall."
}
