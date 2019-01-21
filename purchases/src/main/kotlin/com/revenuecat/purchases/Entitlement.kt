//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.os.Parcel
import android.os.Parcelable

/**
 * An entitlement represents features or content that a user is "entitled" to.
 * Entitlements are unlocked by having an active subscription or making a one-time purchase.
 * Many different products can unlock. Most subscription apps only have one entitlement,
 * unlocking all premium features. However, if you had two tiers of content such as premium and
 * premium_plus, you would have 2 entitlements. A common and simple setup example is one entitlement
 * with identifier pro, one offering monthly, with one product.
 * See [this link](https://docs.revenuecat.com/docs/entitlements) for more info
 * @property offerings Map of offering objects by name
 */
class Entitlement internal constructor(
    val offerings: Map<String, Offering>
) : Parcelable {

    /**
     * @hide
     */
    constructor(parcel: Parcel) : this(
        parcel.readInt().let { size ->
            (0 until size).map {
                parcel.readString() to parcel.readParcelable<Offering>(Offering::class.java.classLoader)
            }.toMap()
        }
    )

    /**
     * @hide
     */
    override fun toString(): String =
        "<Entitlement offerings: {\n" +
                offerings.map { (_, offering) ->
                    "\t $offering => {activeProduct: ${offering.activeProductIdentifier}, " +
                            "loaded: ${offering.skuDetails?.let { "YES" } ?: "NO" }\n"
                } +
                "} >"

    /**
     * @hide
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(offerings.size)
        offerings.forEach { (key, offering) ->
            parcel.writeString(key)
            parcel.writeParcelable(offering, flags)
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
    companion object CREATOR : Parcelable.Creator<Entitlement> {
        /**
         * @hide
         */
        override fun createFromParcel(parcel: Parcel): Entitlement {
            return Entitlement(parcel)
        }
        /**
         * @hide
         */
        override fun newArray(size: Int): Array<Entitlement?> {
            return arrayOfNulls(size)
        }
    }
}
