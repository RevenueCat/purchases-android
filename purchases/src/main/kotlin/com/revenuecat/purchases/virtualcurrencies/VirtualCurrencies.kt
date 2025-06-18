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
 * @property all
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
     * Returns a map containing only the virtual currencies that have a balance greater than zero.
     * @return A map of virtual currency codes to their corresponding info objects,
     *     filtered to only include those with non-zero balances.
     */
    val withNonZeroBalance: Map<String, VirtualCurrency>
        get() = all.filterValues { it.balance > 0 }

    /**
     * Returns a map containing only the virtual currencies that have a balance of zero.
     * @return A map of virtual currency codes to their corresponding info objects,
     *     filtered to only include those with zero balances.
     */
    val withZeroBalance: Map<String, VirtualCurrency>
        get() = all.filterValues { it.balance == 0 }

    /**
     * Returns the virtual currency for the given key, or null if it doesn't exist.
     *
     * @param code The code of the virtual currency to retrieve
     * @return The virtual currency, or null if not found
     */
    operator fun get(code: String): VirtualCurrency? = all[code]

//    companion object {
//        internal fun fromJson(json: JSONObject): VirtualCurrencies {
//            val vcsJson = json.optJSONObject("virtual_currencies") ?: json
//            val all = mutableMapOf<String, VirtualCurrency>()
//            val keys = vcsJson.keys()
//            while (keys.hasNext()) {
//                val key = keys.next()
//                val currencyJson = vcsJson.getJSONObject(key)
//                val currency = VirtualCurrency.fromJson(currencyJson)
//                all[key] = currency
//            }
//            return VirtualCurrencies(all = all)
//        }
//    }

    @IgnoredOnParcel
    override val rawData: JSONObject
        get() = jsonObject
}
