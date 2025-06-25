package com.revenuecat.purchases.virtualcurrencies

import android.os.Parcelable
import com.revenuecat.purchases.models.RawDataContainer
import com.revenuecat.purchases.utils.JSONObjectParceler
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

/**
 * This class contains all the virtual currencies associated to the user.
 *
 * @property all - All of the virtual currencies associated to the user.
 */
@Poko
@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
class VirtualCurrencies internal constructor(
    val all: Map<String, VirtualCurrency>,
    private val jsonObject: JSONObject,
) : Parcelable, RawDataContainer<JSONObject> {
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

    @IgnoredOnParcel
    override val rawData: JSONObject
        get() = jsonObject
}
