package com.revenuecat.purchases

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Offerings(
    val current: Offering?,
    internal val availableOfferings: Map<String, Offering>
) : Parcelable {

    @Suppress("MemberVisibilityCanBePrivate")
    fun getOffering(identifier: String) = availableOfferings[identifier]

    operator fun get(s: String) = getOffering(s)

}