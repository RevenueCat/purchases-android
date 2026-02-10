@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.state

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer

/**
 * Interface for component styles that can exist within a package or tab context.
 * Provides the information needed to determine selection state and offer eligibility
 * for applying conditional overrides.
 */
internal interface PackageContext {
    /** The package this component is associated with, or null if outside a package scope. */
    val rcPackage: Package?

    /** The tab index this component is associated with, or null if not in a tab control. */
    val tabIndex: Int?

    /** The resolved offer for this package, containing subscription option and promo offer status. */
    val resolvedOffer: ResolvedOffer?

    /** Pre-computed offer eligibility for applying intro/promo offer conditional overrides. */
    val offerEligibility: OfferEligibility?

    /**
     * Unique identifier combining package ID and offer ID.
     * This distinguishes multiple components referencing the same package
     * but with different offer configurations.
     */
    val packageUniqueId: String?
        get() {
            val pkg = rcPackage ?: return null
            val offerId = (resolvedOffer as? ResolvedOffer.ConfiguredOffer)?.option?.id
            return if (offerId != null) "${pkg.identifier}:$offerId" else pkg.identifier
        }

    @JvmSynthetic
    fun computeIsSelected(
        selectedPackageInfo: PaywallState.Loaded.Components.SelectedPackageInfo?,
        selectedTabIndex: Int,
    ): Boolean {
        val packageUniqueId = this.packageUniqueId
        val rcPackage = this.rcPackage
        val tabIndex = this.tabIndex
        return when {
            packageUniqueId != null -> packageUniqueId == selectedPackageInfo?.uniqueId
            rcPackage != null -> rcPackage.identifier == selectedPackageInfo?.rcPackage?.identifier
            tabIndex != null -> tabIndex == selectedTabIndex
            else -> false
        }
    }

    @JvmSynthetic
    fun resolveOfferEligibility(
        selectedOfferEligibility: OfferEligibility,
    ): OfferEligibility {
        return this.offerEligibility ?: selectedOfferEligibility
    }
}
