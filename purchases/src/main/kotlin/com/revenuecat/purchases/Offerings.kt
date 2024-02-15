package com.revenuecat.purchases

/**
 * This class contains all the offerings configured in RevenueCat dashboard.
 * For more info see https://docs.revenuecat.com/docs/entitlements
 * @property current Current offering configured in the RevenueCat dashboard.
 * @property all Dictionary of all Offerings [Offering] objects keyed by their identifier.
 */
data class Offerings(
    val current: Offering?,
    val all: Map<String, Offering>,
    val placement: Placements?,
) {

    /**
     * Retrieves an specific offering by its identifier.
     * @param identifier Offering identifier
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun getOffering(identifier: String) = all[identifier]

    /**
     * Retrieves an specific offering by its identifier. It's equivalent to
     * calling [getOffering(identifier)]
     * @param identifier Offering identifier
     */
    operator fun get(identifier: String) = getOffering(identifier)

    /**
     * Retrieves an specific offering by a placement identifier.
     * For more info see https://www.revenuecat.com/docs/tools/targeting
     * @param placementId Placement identifier
     */
    fun getCurrentOfferingForPlacement(placementId: String): Offering? {
        val placement = this.placement ?: run {
            return null
        }

        val offeringForPlacement = placement.offeringIdsByPlacement[placementId]?.let { getOffering(it) }
        val fallbackOffering = placement.fallbackOfferingId?.let { getOffering(it) }

        val showNoOffering = placement.offeringIdsByPlacement.containsKey(placementId)
        val offering = offeringForPlacement ?: if (showNoOffering) null else fallbackOffering

        return offering?.withPlacement(placementId)
    }
}

/**
 * This class contains all the offerings by placement configured in RevenueCat dashboard.
 * For more info see https://www.revenuecat.com/docs/targeting
 * @property fallbackOfferingId The optional offering identifier to fallback on if the placement isn't found.
 * @property offeringIdsByPlacement Dictionary of all offering identifiers keyed by their placement identifier.
 */
data class Placements(
    val fallbackOfferingId: String?,
    val offeringIdsByPlacement: Map<String, String?>,
)

private fun Offering.withPlacement(placementId: String): Offering {
    val updatedAvailablePackages = this.availablePackages.map {
        val context = PresentedOfferingContext(
            offeringIdentifier = it.presentedOfferingContext.offeringIdentifier,
            placementIdentifier = placementId,
        )
        val product = it.product.copyWithPresentedOfferingContext(context)

        Package(
            identifier = it.identifier,
            packageType = it.packageType,
            product = product,
            presentedOfferingContext = context,
        )
    }

    return Offering(
        identifier = this.identifier,
        serverDescription = this.serverDescription,
        metadata = this.metadata,
        availablePackages = updatedAvailablePackages,
        paywall = this.paywall,
    )
}
