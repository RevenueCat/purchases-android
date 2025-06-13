package com.revenuecat.purchases

import android.os.Parcelable
import com.revenuecat.purchases.utils.getNullableString
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * This class contains all the virtual currencies associated to the user.
 *
 * @property all
 */
@Poko
@Parcelize
class VirtualCurrencies internal constructor(
    val all: Map<String, VirtualCurrency>
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

    companion object {
        internal fun fromJson(json: JSONObject): VirtualCurrencies {
            val vcsJson = json.optJSONObject("virtual_currencies") ?: json
            val all = mutableMapOf<String, VirtualCurrency>()
            val keys = vcsJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val currencyJson = vcsJson.getJSONObject(key)
                val currency = VirtualCurrency.fromJson(currencyJson)
                all[key] = currency
            }
            return VirtualCurrencies(all = all)
        }
    }
}
