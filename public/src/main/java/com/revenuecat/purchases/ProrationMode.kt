package com.revenuecat.purchases

import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel

/**
 * Contains information about the proration mode to use in case of a product upgrade.
 * Use the platform specific subclasses in each implementation.
 * @property name Identifier of the proration mode to be used
 */
@Deprecated(
    "Replaced with ReplacementMode",
    ReplaceWith("ReplacementMode"),
)
interface ProrationMode : Parcelable {
    @IgnoredOnParcel
    val name: String
}
