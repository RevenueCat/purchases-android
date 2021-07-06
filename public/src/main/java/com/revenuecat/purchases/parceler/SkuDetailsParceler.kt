package com.revenuecat.purchases.parceler

import android.os.Parcel
import android.os.ParcelFormatException
import com.android.billingclient.api.SkuDetails
import kotlinx.android.parcel.Parceler

/** @suppress */
internal object SkuDetailsParceler :
    Parceler<SkuDetails> {

    override fun create(parcel: Parcel): SkuDetails {
        val readString = parcel.readString() ?: throw ParcelFormatException("SkuDetails parcel is a null string")
        return SkuDetails(readString)
    }

    override fun SkuDetails.write(parcel: Parcel, flags: Int) {
        parcel.writeString(originalJson)
    }
}
