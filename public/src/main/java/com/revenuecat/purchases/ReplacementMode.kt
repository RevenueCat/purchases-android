package com.revenuecat.purchases

import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel

/**
 * Contains information about the replacement mode to use in case of a product upgrade.
 * Use the platform specific subclasses in each implementation.
 * @property name Identifier of the proration mode to be used
 */
interface ReplacementMode : Parcelable {
    @IgnoredOnParcel
    val name: String
}
