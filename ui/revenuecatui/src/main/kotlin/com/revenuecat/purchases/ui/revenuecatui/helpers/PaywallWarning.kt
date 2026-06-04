package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError

/**
 * Represents a warning to display when the default paywall is shown.
 * Only displayed in DEBUG builds.
 */
internal sealed class PaywallWarning {
    object NoOffering : PaywallWarning()
    data class NoProducts(val error: Throwable) : PaywallWarning()
    data class NoPaywall(val offeringId: String) : PaywallWarning()
    object MissingLocalization : PaywallWarning()
    object MissingTiers : PaywallWarning()
    data class MissingTier(val tierId: String) : PaywallWarning()
    data class MissingTierName(val tierId: String) : PaywallWarning()
    data class InvalidTemplate(val templateName: String) : PaywallWarning()
    data class InvalidVariables(val variables: Set<String>) : PaywallWarning()
    data class InvalidIcons(val icons: Set<String>) : PaywallWarning()
    data class Other(val message: String) : PaywallWarning()

    val title: String
        get() = when (this) {
            is NoPaywall -> "No Paywall configured"
            is NoOffering -> "No Offering found"
            is NoProducts -> "Could not fetch products"
            is MissingLocalization -> "Missing localization"
            is MissingTiers -> "No Tiers"
            is MissingTier -> "Tier is missing localization"
            is MissingTierName -> "Tier $tierId is missing a name"
            is InvalidTemplate -> "Unknown Template"
            is InvalidVariables -> "Unrecognized variables"
            is InvalidIcons -> "Invalid icon names"
            is Other -> "Paywall Misconfigured"
        }

    val bodyText: String
        get() = when (this) {
            is NoPaywall ->
                "Your `$offeringId` offering has no configured paywalls. " +
                    "Set one up in the RevenueCat Dashboard to begin."

            is NoOffering ->
                "We could not detect any offerings. " +
                    "Set one up in the RevenueCat Dashboard to begin."

            is NoProducts ->
                "We could not fetch any products: " +
                    (error.localizedMessage ?: error.message ?: "Unknown error")

            is MissingLocalization ->
                "Your paywall is missing a localization. " +
                    "Add a localization in the RevenueCat Dashboard to begin."

            is MissingTiers ->
                "Your paywall is missing any tiers. " +
                    "Add some tiers in the RevenueCat Dashboard to begin."

            is MissingTier ->
                "The tier with ID: $tierId is missing a localization. " +
                    "Add a localization in the RevenueCat Dashboard to begin."

            is MissingTierName ->
                "The tier: $tierId is missing a name. " +
                    "Add a name in the RevenueCat Dashboard to continue."

            is InvalidTemplate ->
                "The template with ID: `$templateName` does not exist for this version of the SDK. " +
                    "Please make sure to update your SDK to the latest version and try again."

            is InvalidVariables -> {
                val variables = variables.sorted().joinToString(", ")
                "The following variables are not recognized: $variables. " +
                    "Please check the docs for a list of valid variables."
            }

            is InvalidIcons -> {
                val icons = icons.sorted().joinToString(", ")
                "The following icon names are not valid: $icons. " +
                    "Please check `PaywallIcon` for the list of valid icon names."
            }

            is Other -> "Paywall validation failed with message: $message"
        }

    val helpUrl: String?
        get() = when (this) {
            is NoPaywall, is MissingTierName, is MissingTier, is MissingTiers ->
                "https://www.revenuecat.com/docs/tools/paywalls"

            is NoOffering ->
                "https://www.revenuecat.com/docs/offerings/overview"

            is NoProducts ->
                "https://www.revenuecat.com/docs/offerings/products-overview"

            is InvalidVariables ->
                "https://www.revenuecat.com/docs/tools/paywalls/creating-paywalls/variables"

            else -> null
        }

    companion object {
        fun from(error: PaywallValidationError): PaywallWarning = when (error) {
            is PaywallValidationError.MissingPaywall -> NoPaywall("unknown")
            is PaywallValidationError.InvalidTemplate -> InvalidTemplate(error.templateName)
            is PaywallValidationError.InvalidVariables -> InvalidVariables(error.unrecognizedVariables)
            is PaywallValidationError.InvalidIcons -> InvalidIcons(error.invalidIcons)
            is PaywallValidationError.MissingTiers -> MissingTiers
            is PaywallValidationError.MissingTierConfigurations -> MissingTier(
                error.tierIds.firstOrNull() ?: "unknown",
            )

            is PaywallValidationError.MissingStringLocalization,
            is PaywallValidationError.MissingVideoLocalization,
            is PaywallValidationError.AllVariableLocalizationsMissing,
            is PaywallValidationError.AllLocalizationsMissing,
            is PaywallValidationError.MissingImageLocalization,
            -> MissingLocalization

            else -> Other(error.message ?: "Unknown error")
        }
    }
}
