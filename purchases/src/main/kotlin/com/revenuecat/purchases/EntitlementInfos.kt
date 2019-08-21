package com.revenuecat.purchases

import android.os.Parcel
import android.os.Parcelable

class EntitlementInfos internal constructor(
    val all: Map<String, EntitlementInfo>
) : Parcelable {

    val active = all.filter { it.value.isActive }

    operator fun get(s: String) = all[s]

    constructor(parcel: Parcel) : this(
        all = parcel.readStringParcelableMap(EntitlementInfo::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeStringParcelableMap(all)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntitlementInfos

        if (all != other.all) return false
        if (active != other.active) return false

        return true
    }

    companion object {
        @JvmField val CREATOR = object : Parcelable.Creator<EntitlementInfos> {
            override fun createFromParcel(parcel: Parcel): EntitlementInfos {
                return EntitlementInfos(parcel)
            }

            override fun newArray(size: Int): Array<EntitlementInfos?> {
                return arrayOfNulls(size)
            }
        }
    }
}