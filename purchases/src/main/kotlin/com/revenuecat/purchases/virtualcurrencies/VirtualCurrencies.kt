package com.revenuecat.purchases.virtualcurrencies

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * This class contains all the virtual currencies associated to the user.
 *
 * @property all - All of the virtual currencies associated to the user.
 */
@Poko
@Parcelize
@Serializable
class VirtualCurrencies internal constructor(
    @SerialName("virtual_currencies")
    val all: Map<String, VirtualCurrency>,
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VirtualCurrencies
        return all == other.all
    }

    override fun hashCode(): Int {
        return all.hashCode()
    }

    /**
     * Returns the virtual currency for the given key, or null if it doesn't exist.
     *
     * @param code The code of the virtual currency to retrieve
     * @return The virtual currency, or null if not found
     */
    operator fun get(code: String): VirtualCurrency? = all[code]
}
