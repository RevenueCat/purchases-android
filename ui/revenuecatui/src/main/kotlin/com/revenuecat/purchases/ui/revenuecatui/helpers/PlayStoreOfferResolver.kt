package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.paywalls.components.common.PlayStoreOfferConfig

/**
 * Result of resolving a Play Store offer configuration.
 */
@InternalRevenueCatAPI
sealed class ResolvedOffer {
    /**
     * A specific offer was configured and successfully resolved.
     */
    data class ConfiguredOffer(val option: SubscriptionOption) : ResolvedOffer()

    /**
     * No offer configuration was provided, using default behavior.
     */
    data class NoConfiguration(val option: SubscriptionOption?) : ResolvedOffer()

    /**
     * An offer was configured but could not be found in the available subscription options.
     * Falls back to the default option but logs a warning.
     */
    data class ConfigurationError(
        val configuredOfferId: String,
        val fallbackOption: SubscriptionOption?,
    ) : ResolvedOffer()

    /**
     * Gets the resolved subscription option, or null if resolution failed entirely.
     */
    val subscriptionOption: SubscriptionOption?
        get() = when (this) {
            is ConfiguredOffer -> option
            is NoConfiguration -> option
            is ConfigurationError -> fallbackOption
        }

    /**
     * Whether this resolved offer is a promo offer (specifically configured Play Store offer).
     */
    val isPromoOffer: Boolean
        get() = this is ConfiguredOffer
}

/**
 * Resolves the subscription option to use for a package based on its Play Store offer configuration.
 */
@InternalRevenueCatAPI
object PlayStoreOfferResolver {

    /**
     * Resolves the subscription option to use for a package based on the offer configuration.
     *
     * @param rcPackage The package to resolve the offer for
     * @param offerConfig The Play Store offer configuration, or null if not configured
     * @return The resolved offer result
     */
    fun resolve(
        rcPackage: Package,
        offerConfig: PlayStoreOfferConfig?,
    ): ResolvedOffer {
        val subscriptionOptions = rcPackage.product.subscriptionOptions
        val defaultOption = rcPackage.product.defaultOption

        if (subscriptionOptions == null || offerConfig == null) {
            return ResolvedOffer.NoConfiguration(defaultOption)
        }

        val configuredOption = findOfferById(subscriptionOptions, offerConfig.offerId)
        return if (configuredOption != null) {
            ResolvedOffer.ConfiguredOffer(configuredOption)
        } else {
            Logger.w(
                "Configured Play Store offer '${offerConfig.offerId}' not found for package " +
                    "'${rcPackage.identifier}'. Falling back to default option.",
            )
            ResolvedOffer.ConfigurationError(
                configuredOfferId = offerConfig.offerId,
                fallbackOption = defaultOption,
            )
        }
    }

    /**
     * Finds a subscription option by offer ID.
     *
     * @param subscriptionOptions The list of subscription options to search
     * @param offerId The offer ID to find
     * @return The matching subscription option, or null if not found
     */
    private fun findOfferById(
        subscriptionOptions: List<SubscriptionOption>,
        offerId: String,
    ): SubscriptionOption? {
        return subscriptionOptions.firstOrNull { option ->
            // Check if this is a GoogleSubscriptionOption with matching offerId
            (option as? GoogleSubscriptionOption)?.offerId == offerId
        }
    }
}
