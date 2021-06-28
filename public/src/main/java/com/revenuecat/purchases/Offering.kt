//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

/**
 * An offering is a collection of [Package] available for the user to purchase.
 * For more info see https://docs.revenuecat.com/docs/entitlements
 * @property identifier Unique identifier defined in RevenueCat dashboard.
 * @property serverDescription Offering description defined in RevenueCat dashboard.
 * @property availablePackages Array of [Package] objects available for purchase.
 */
@Parcelize
data class Offering constructor(
    val identifier: String,
    val serverDescription: String,
    val availablePackages: List<Package>
) : Parcelable {

    /**
     * Lifetime package type configured in the RevenueCat dashboard, if available.
     */
    @IgnoredOnParcel val lifetime by lazy { findPackage(PackageType.LIFETIME) }

    /**
     * Annual package type configured in the RevenueCat dashboard, if available.
     */
    @IgnoredOnParcel val annual by lazy { findPackage(PackageType.ANNUAL) }

    /**
     * Six month package type configured in the RevenueCat dashboard, if available.
     */
    @IgnoredOnParcel val sixMonth by lazy { findPackage(PackageType.SIX_MONTH) }

    /**
     * Three month package type configured in the RevenueCat dashboard, if available.
     */
    @IgnoredOnParcel val threeMonth by lazy { findPackage(PackageType.THREE_MONTH) }

    /**
     * Two month package type configured in the RevenueCat dashboard, if available.
     */
    @IgnoredOnParcel val twoMonth by lazy { findPackage(PackageType.TWO_MONTH) }

    /**
     * Monthly package type configured in the RevenueCat dashboard, if available.
     */
    @IgnoredOnParcel val monthly by lazy { findPackage(PackageType.MONTHLY) }

    /**
     * Weekly package type configured in the RevenueCat dashboard, if available.
     */
    @IgnoredOnParcel val weekly by lazy { findPackage(PackageType.WEEKLY) }

    private fun findPackage(packageType: PackageType) =
        availablePackages.firstOrNull { it.identifier == packageType.identifier }

    /**
     * Retrieves a specific package by identifier, use this to access custom package types configured
     * in the RevenueCat dashboard. Equivalent to calling `getPackage`.
     */
    operator fun get(s: String) = getPackage(s)

    /**
     * Retrieves a specific package by identifier, use this to access custom package types configured
     * in the RevenueCat dashboard
     * @throws NoSuchElementException if there's no package with the specified identifier in the Offering.
     */
    @Throws(NoSuchElementException::class)
    @Suppress("MemberVisibilityCanBePrivate")
    fun getPackage(identifier: String) =
        availablePackages.first { it.identifier == identifier }
}
