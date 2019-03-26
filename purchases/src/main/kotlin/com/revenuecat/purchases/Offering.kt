//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.os.Parcel
import android.os.Parcelable
import com.android.billingclient.api.SkuDetails

/**
 * Most well monetized subscription apps provide many different offerings to purchase an
 * entitlement. These are usually associated with different durations i.e. an annual plan and a
 * monthly plan.
 * See [this link](https://docs.revenuecat.com/docs/entitlements) for more info
 * @property activeProductIdentifier The currently active Play Store product for this offering
 * @property skuDetails Object containing an in-app product's or subscription's listing details
 */
data class Offering @JvmOverloads internal constructor(
    val activeProductIdentifier: String,
    var skuDetails: SkuDetails? = null
) : Parcelable {

    /**
     * @hide
     */
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString().takeUnless { it.isNullOrBlank() }?.let { SkuDetails(it) }
    )

    /**
     * @hide
     */
    override fun toString() =
        "<Offering activeProductIdentifier: $activeProductIdentifier, skuDetails: $skuDetails>"

    /**
     * @hide
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(activeProductIdentifier)
        if (skuDetails != null) {
            try {
                val field = SkuDetails::class.java.getDeclaredField("mOriginalJson")
                field.isAccessible = true
                val value = field.get(skuDetails) as String
                parcel.writeString(value)
            } catch (e: NoSuchFieldException) {
                log("Error converting SkuDetails to Parcelable")
                parcel.writeString("")
            }
        } else {
            parcel.writeString("")
        }
    }

    /**
     * @hide
     */
    override fun describeContents(): Int {
        return 0
    }

    /**
     * @hide
     */
    companion object CREATOR : Parcelable.Creator<Offering> {
        /**
         * @hide
         */
        override fun createFromParcel(parcel: Parcel): Offering {
            return Offering(parcel)
        }
        /**
         * @hide
         */
        override fun newArray(size: Int): Array<Offering?> {
            return arrayOfNulls(size)
        }
    }
}
