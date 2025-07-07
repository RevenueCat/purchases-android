package com.revenuecat.purchases.virtualcurrencies

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A class representing information about a virtual currency in the app.
 *
 * @property balance The customer's current balance of the virtual currency.
 * @property name The virtual currency's name defined in the RevenueCat dashboard.
 * @property code The virtual currency's code defined in the RevenueCat dashboard.
 * @property serverDescription The virtual currency description defined in the RevenueCat dashboard.
 */
@Poko
@Serializable
@Parcelize
class VirtualCurrency internal constructor(
    val balance: Int,
    val name: String,
    val code: String,
    @SerialName("description")
    val serverDescription: String? = null,
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VirtualCurrency
        return balance == other.balance &&
            name == other.name &&
            code == other.code &&
            serverDescription == other.serverDescription
    }

    override fun hashCode(): Int {
        var result = balance.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + code.hashCode()
        result = 31 * result + (serverDescription?.hashCode() ?: 0)
        return result
    }
}
