package com.revenuecat.purchases

import com.revenuecat.purchases.common.HTTPResponseOriginalSource
import dev.drewhamilton.poko.Poko

/**
 * This class contains all the offerings configured in RevenueCat dashboard.
 * For more info see https://docs.revenuecat.com/docs/entitlements
 * @property current Current offering configured in the RevenueCat dashboard.
 * @property all Dictionary of all Offerings [Offering] objects keyed by their identifier.
 */
@Poko
public class Offerings internal constructor(
    public val current: Offering?,
    public val all: Map<String, Offering>,
    internal val placements: Placements? = null,
    internal val targeting: Targeting? = null,
    internal val originalSource: HTTPResponseOriginalSource = HTTPResponseOriginalSource.MAIN,
    internal val loadedFromDiskCache: Boolean = false,
) {
    public constructor(current: Offering?, all: Map<String, Offering>) : this(current, all, null, null)

    /**
     * Retrieves an specific offering by its identifier.
     * @param identifier Offering identifier
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public fun getOffering(identifier: String): Offering? = all[identifier]

    /**
     * Retrieves an specific offering by its identifier. It's equivalent to
     * calling [getOffering(identifier)]
     * @param identifier Offering identifier
     */
    public operator fun get(identifier: String): Offering? = getOffering(identifier)

    /**
     * Retrieves an specific offering by a placement identifier.
     * For more info see https://www.revenuecat.com/docs/tools/targeting
     * @param placementId Placement identifier
     */
    public fun getCurrentOfferingForPlacement(placementId: String): Offering? {
        val placements = this.placements ?: run {
            return null
        }

        val offeringForPlacement = placements.offeringIdsByPlacement[placementId]?.let { getOffering(it) }
        val fallbackOffering = placements.fallbackOfferingId?.let { getOffering(it) }

        val showNoOffering = placements.offeringIdsByPlacement.containsKey(placementId)
        val offering = offeringForPlacement ?: if (showNoOffering) null else fallbackOffering

        return offering?.withPresentedContext(placementId = placementId, targeting = this.targeting)
    }

    /**
     * This class contains information about the targeting that presented the offering.
     * @property revision The revision of the targeting that presented the offering.
     * @property ruleId The rule id of the targeting that presented the offering.
     */
    internal data class Targeting(
        val revision: Int,
        val ruleId: String,
    )

    /**
     * This class contains all the offerings by placement configured in RevenueCat dashboard.
     * For more info see https://www.revenuecat.com/docs/targeting
     * @property fallbackOfferingId The optional offering identifier to fallback on if the placement isn't found.
     * @property offeringIdsByPlacement Dictionary of all offering identifiers keyed by their placement identifier.
     */
    internal data class Placements(
        val fallbackOfferingId: String?,
        val offeringIdsByPlacement: Map<String, String?>,
    )

    internal fun copy(
        current: Offering? = this.current,
        all: Map<String, Offering> = this.all,
        placements: Placements? = this.placements,
        targeting: Targeting? = this.targeting,
        originalSource: HTTPResponseOriginalSource = this.originalSource,
        loadedFromDiskCache: Boolean = this.loadedFromDiskCache,
    ): Offerings {
        return Offerings(
            current = current,
            all = all,
            placements = placements,
            targeting = targeting,
            originalSource = originalSource,
            loadedFromDiskCache = loadedFromDiskCache,
        )
    }

    /**
     * @hide
     */
    public override fun toString(): String =
        "<Offerings\n " +
            "current: $current\n" +
            "all:  $all,\n" +
            "placements: $placements,\n" +
            "targeting: $targeting\n>"
    public override fun equals(other: Any?): Boolean = other is Offerings &&
        OfferingsComparableData(this) == OfferingsComparableData(other)
    public override fun hashCode(): Int = OfferingsComparableData(this).hashCode()
}

/**
 * Contains fields to be used for equality, which ignores originalSource and loadedFromDiskCache.
 */
private data class OfferingsComparableData(
    val current: Offering?,
    val all: Map<String, Offering>,
    val placements: Offerings.Placements?,
    val targeting: Offerings.Targeting?,
) {
    constructor(
        offerings: Offerings,
    ) : this(
        current = offerings.current,
        all = offerings.all,
        placements = offerings.placements,
        targeting = offerings.targeting,
        // Note: originalSource and loadedFromDiskCache are excluded from equality comparison as they are metadata
    )
}

@OptIn(InternalRevenueCatAPI::class)
internal fun Offering.withPresentedContext(placementId: String?, targeting: Offerings.Targeting?): Offering {
    val updatedAvailablePackages = this.availablePackages.map {
        val oldContext = it.presentedOfferingContext

        val newContext = oldContext.copy(
            placementIdentifier = placementId ?: oldContext.placementIdentifier,
            targetingContext = targeting?.let { targeting ->
                PresentedOfferingContext.TargetingContext(targeting.revision, targeting.ruleId)
            } ?: oldContext.targetingContext,
        )
        val product = it.product.copyWithPresentedOfferingContext(newContext)

        Package(
            identifier = it.identifier,
            packageType = it.packageType,
            product = product,
            presentedOfferingContext = newContext,
            webCheckoutURL = it.webCheckoutURL,
        )
    }

    return Offering(
        identifier = this.identifier,
        serverDescription = this.serverDescription,
        metadata = this.metadata,
        availablePackages = updatedAvailablePackages,
        paywall = this.paywall,
        paywallComponents = this.paywallComponents,
        webCheckoutURL = this.webCheckoutURL,
    )
}
