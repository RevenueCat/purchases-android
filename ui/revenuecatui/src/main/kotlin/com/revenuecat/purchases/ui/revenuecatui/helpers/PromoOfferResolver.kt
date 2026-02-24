package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.paywalls.components.common.PromoOfferConfig

/**
 * Result of resolving a Play Store offer configuration.
 */
internal sealed class ResolvedOffer {
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
     * This can happen due to misconfiguration or because the user is not eligible for the offer.
     * Falls back to the default option.
     */
    data class OfferNotFound(
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
            is OfferNotFound -> fallbackOption
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
internal object PromoOfferResolver {

    /**
     * Resolves the subscription option to use for a package based on the offer configuration.
     *
     * @param rcPackage The package to resolve the offer for
     * @param offerConfig The promo offer configuration, or null if not configured
     * @return The resolved offer result
     */
    fun resolve(
        rcPackage: Package,
        offerConfig: PromoOfferConfig?,
    ): ResolvedOffer {
        val defaultOption = rcPackage.product.defaultOption

        if (offerConfig == null) {
            return ResolvedOffer.NoConfiguration(defaultOption)
        }

        val subscriptionOptions = rcPackage.product.subscriptionOptions
        val configuredOption = subscriptionOptions?.let { findOfferById(it, offerConfig.offerId) }

        return if (configuredOption != null) {
            ResolvedOffer.ConfiguredOffer(configuredOption)
        } else {
            val reason = if (subscriptionOptions == null) {
                "product has no subscription options"
            } else {
                "offer not found in available options"
            }
            Logger.w(
                "Configured offer '${offerConfig.offerId}' for package '${rcPackage.identifier}': " +
                    "$reason. Falling back to default option.",
            )
            ResolvedOffer.OfferNotFound(
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
