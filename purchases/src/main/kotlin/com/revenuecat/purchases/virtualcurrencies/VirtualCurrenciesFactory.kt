package com.revenuecat.purchases.virtualcurrencies

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.common.networking.HTTPResult
import kotlinx.serialization.SerializationException
import org.json.JSONException
import org.json.JSONObject

/**
 * Builds a [VirtualCurrencies] object.
 *
 * @throws [JSONException] If the JSON is invalid.
 * @throws [SerializationException] In case of any decoding-specific error
 * @throws [IllegalArgumentException] - if the decoded input is not a valid instance of [VirtualCurrencies]
 */
internal object VirtualCurrenciesFactory {

    @OptIn(InternalRevenueCatAPI::class)
    @Throws(JSONException::class, SerializationException::class, IllegalArgumentException::class)
    fun buildVirtualCurrencies(httpResult: HTTPResult): VirtualCurrencies {
        return buildVirtualCurrencies(
            body = httpResult.body,
        )
    }

    @Throws(JSONException::class, SerializationException::class, IllegalArgumentException::class)
    fun buildVirtualCurrencies(
        body: JSONObject,
    ): VirtualCurrencies {
        return JsonProvider.defaultJson.decodeFromString<VirtualCurrencies>(
            body.toString(),
        )
    }

    @Throws(JSONException::class, SerializationException::class, IllegalArgumentException::class)
    fun buildVirtualCurrencies(
        jsonString: String,
    ): VirtualCurrencies {
        return JsonProvider.defaultJson.decodeFromString<VirtualCurrencies>(
            jsonString,
        )
    }
}
