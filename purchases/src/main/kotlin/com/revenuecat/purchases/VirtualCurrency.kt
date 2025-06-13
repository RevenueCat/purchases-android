package com.revenuecat.purchases

import android.os.Parcelable
import com.revenuecat.purchases.utils.getNullableString
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * A class representing information about a virtual currency in the app.
 *
 * @property balance The customer's current balance of the virtual currency.
 * @property name The virtual currency's name defined in the RevenueCat dashboard.
 * @property code The virtual currency's code defined in the RevenueCat dashboard.
 * @property serverDescription The virtual currency description defined in the RevenueCat dashboard.
 */
@Poko
@Parcelize
class VirtualCurrency internal constructor(
    val balance: Int,
    val name: String,
    val code: String,
    val serverDescription: String?
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VirtualCurrency
        return balance == other.balance
    }

    override fun hashCode(): Int {
        return balance.hashCode()
    }

    companion object {
        internal fun fromJson(json: JSONObject): VirtualCurrency {
            val balance = json.getInt("balance")
            val name = json.getString("name")
            val code = json.getString("code")
            val serverDescription = json.getNullableString("description")
            return VirtualCurrency(
                balance = balance,
                name = name,
                code = code,
                serverDescription = serverDescription
            )
        }
    }
}
