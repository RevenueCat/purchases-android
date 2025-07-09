package com.revenuecat.purchases.virtualcurrencies

import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.common.networking.HTTPResult
import org.json.JSONException
import org.json.JSONObject

/**
 * Builds a [VirtualCurrencies] object.
 *
 * @throws [JSONException] If the JSON is invalid.
 */
internal object VirtualCurrenciesFactory {

    @Throws(JSONException::class)
    fun buildVirtualCurrencies(httpResult: HTTPResult): VirtualCurrencies {
        return VirtualCurrenciesFactory.buildVirtualCurrencies(
            body = httpResult.body,
        )
    }

    @Throws(JSONException::class)
    fun buildVirtualCurrencies(
        body: JSONObject,
    ): VirtualCurrencies {
        return JsonProvider.defaultJson.decodeFromString<VirtualCurrencies>(
            body.toString(),
        )
    }

    @Throws(JSONException::class)
    fun buildVirtualCurrencies(
        jsonString: String,
    ): VirtualCurrencies {
        return JsonProvider.defaultJson.decodeFromString<VirtualCurrencies>(
            jsonString,
        )
    }
}
