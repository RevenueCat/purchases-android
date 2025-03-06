package com.revenuecat.purchases

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * A class representing information about a virtual currency in the app.
 *
 * @property balance The current balance of the virtual currency.
 */
@Poko
@Parcelize
class VirtualCurrencyInfo internal constructor(
    val balance: Long,
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VirtualCurrencyInfo
        return balance == other.balance
    }

    override fun hashCode(): Int {
        return balance.hashCode()
    }

    companion object {
        internal fun fromJson(json: JSONObject): VirtualCurrencyInfo {
            val balance = json.getLong("amount")
            return VirtualCurrencyInfo(balance)
        }
    }
}
