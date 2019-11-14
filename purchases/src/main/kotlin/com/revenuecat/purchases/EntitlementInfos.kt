package com.revenuecat.purchases

import android.os.Parcel
import android.os.Parcelable

/**
 * This class contains all the entitlements associated to the user.
 * @property all Map of all EntitlementInfo [EntitlementInfo] objects (active and inactive) keyed by
 * entitlement identifier.
 */
class EntitlementInfos internal constructor(
    val all: Map<String, EntitlementInfo>
) : Parcelable {

    /**
     * Dictionary of active [EntitlementInfo] objects keyed by entitlement identifier.
     */
    val active = all.filter { it.value.isActive }

    /**
     * Retrieves an specific entitlementInfo by its entitlement identifier. It's equivalent to
     * accessing the `all` map by entitlement identifier.
     */
    operator fun get(s: String) = all[s]

    /** @suppress */
    constructor(parcel: Parcel) : this(
        all = parcel.readStringParcelableMap(EntitlementInfo::class.java.classLoader)
    )

    /** @suppress */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeStringParcelableMap(all)
    }

    /** @suppress */
    override fun describeContents(): Int {
        return 0
    }

    /** @suppress */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntitlementInfos

        if (all != other.all) return false
        if (active != other.active) return false

        return true
    }

    /** @suppress */
    companion object {
        /** @suppress */
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