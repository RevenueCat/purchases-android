package com.revenuecat.purchases.virtualcurrencies

import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.common.responses.SubscriptionInfoResponse
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
    return JsonProvider.defaultJson.decodeFromString<VirtualCurrency>(
        this.toString(),
    )
}

internal object VirtualCurrencyJsonKeys {
    const val BALANCE = "balance"
    const val NAME = "name"
    const val CODE = "code"
    const val SERVER_DESCRIPTION = "description"
}
