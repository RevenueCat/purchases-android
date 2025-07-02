package com.revenuecat.purchases.virtualcurrencies

import org.json.JSONException
import org.json.JSONObject

/**
 * Builds a [VirtualCurrency] object.
 *
 * @throws [JSONException] If the JSON is invalid.
 */
internal object VirtualCurrencyFactory {
    @Throws(JSONException::class)
    fun buildVirtualCurrency(
        body: JSONObject,
    ): VirtualCurrency {
        return body.buildVirtualCurrency()
    }
}

internal fun JSONObject.buildVirtualCurrency(): VirtualCurrency {
    val balance: Int = getInt(VirtualCurrencyJsonKeys.BALANCE)
    val name: String = getString(VirtualCurrencyJsonKeys.NAME)
    val code: String = getString(VirtualCurrencyJsonKeys.CODE)
    val serverDescription: String? = optString(VirtualCurrencyJsonKeys.SERVER_DESCRIPTION).takeIf { it.isNotBlank() }

    return VirtualCurrency(
        balance = balance,
        name = name,
        code = code,
        serverDescription = serverDescription,
    )
}

internal object VirtualCurrencyJsonKeys {
    const val BALANCE = "balance"
    const val NAME = "name"
    const val CODE = "code"
    const val SERVER_DESCRIPTION = "description"
}
