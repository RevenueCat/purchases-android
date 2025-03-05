package com.revenuecat.purchases

import android.os.Parcelable
import com.revenuecat.purchases.models.RawDataContainer
import com.revenuecat.purchases.utils.JSONObjectParceler
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

@Parcelize
data class VirtualCurrencyInfo internal constructor(
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
        fun fromJson(json: JSONObject): VirtualCurrencyInfo {
            val balance = json.getLong("amount")
            return VirtualCurrencyInfo(balance)
        }
    }
}
