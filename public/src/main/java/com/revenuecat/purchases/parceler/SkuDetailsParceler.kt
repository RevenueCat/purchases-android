package com.revenuecat.purchases.parceler

import android.os.Parcel
import com.android.billingclient.api.SkuDetails
import kotlinx.android.parcel.Parceler

/** @suppress */
internal object SkuDetailsParceler :
    Parceler<SkuDetails> {

    override fun create(parcel: Parcel): SkuDetails {
        return SkuDetails(parcel.readString())
    }

    override fun SkuDetails.write(parcel: Parcel, flags: Int) {
        parcel.writeString(originalJson)
    }
}
