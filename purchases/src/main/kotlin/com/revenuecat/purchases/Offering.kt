//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

/**
 * An offering is collection of different Packages that lets the user purchase access in different ways.
 */
@Parcelize
data class Offering internal constructor(
    val identifier: String,
    val serverDescription: String,
    val availablePackages: List<Package>
) : Parcelable {

    @IgnoredOnParcel val lifetime by lazy { findPackage(PackageType.LIFETIME) }
    @IgnoredOnParcel val annual by lazy { findPackage(PackageType.ANNUAL) }
    @IgnoredOnParcel val sixMonth by lazy { findPackage(PackageType.SIX_MONTH) }
    @IgnoredOnParcel val threeMonth by lazy { findPackage(PackageType.THREE_MONTH) }
    @IgnoredOnParcel val twoMonth by lazy { findPackage(PackageType.TWO_MONTH) }
    @IgnoredOnParcel val monthly by lazy { findPackage(PackageType.MONTHLY) }
    @IgnoredOnParcel val weekly by lazy { findPackage(PackageType.WEEKLY) }

    private fun findPackage(packageType: PackageType) =
        availablePackages.firstOrNull { it.identifier == packageType.identifier }

    operator fun get(s: String) = getPackage(s)

    @Suppress("MemberVisibilityCanBePrivate")
    fun getPackage(identifier: String) =
        availablePackages.first { it.identifier == identifier }

}
// TODO: make Parcelable
