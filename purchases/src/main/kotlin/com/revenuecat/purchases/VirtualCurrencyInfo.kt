package com.revenuecat.purchases

import android.os.Parcelable
import com.revenuecat.purchases.models.RawDataContainer
import com.revenuecat.purchases.utils.JSONObjectParceler
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class VirtualCurrencyInfo(
    val balance: Long,
    private val jsonObject: JSONObject
): Parcelable, RawDataContainer<JSONObject> {

    @IgnoredOnParcel
    override val rawData: JSONObject
        get() = jsonObject

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
            return VirtualCurrencyInfo(balance, json)
        }
    }
}