package com.revenuecat.purchases

import android.os.Parcelable
import com.android.billingclient.api.SkuDetails
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler

@Parcelize
@TypeParceler<SkuDetails, SkuDetailsParceler>()
data class Package(
    val identifier: String,
    val packageType: PackageType,
    val product: SkuDetails,
    @JvmSynthetic internal val offering: String
) : Parcelable

enum class PackageType(val identifier: String?) {
    CUSTOM(null),
    LIFETIME("\$rc_lifetime"),
    ANNUAL("\$rc_annual"),
    SIX_MONTH("\$rc_six_month"),
    THREE_MONTH("\$rc_three_month"),
    TWO_MONTH("\$rc_two_month"),
    MONTHLY("\$rc_monthly"),
    WEEKLY("\$rc_weekly"),
}

internal fun String.toPackageType(): PackageType =
    PackageType.values().firstOrNull { it.identifier == this } ?: PackageType.CUSTOM