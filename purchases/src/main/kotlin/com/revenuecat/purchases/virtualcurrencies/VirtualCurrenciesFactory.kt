package com.revenuecat.purchases.virtualcurrencies

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
        val virtualCurrenciesObject = body.getJSONObject(VirtualCurrenciesResponseJsonKeys.VIRTUAL_CURRENCIES)
        return virtualCurrenciesObject.buildVirtualCurrencies()
    }
}

internal fun JSONObject.buildVirtualCurrencies(): VirtualCurrencies {
    val all = HashMap<String, VirtualCurrency>()
    keys().forEach { vcCode ->
        val vcObject = getJSONObject(vcCode)
        val vc = VirtualCurrencyFactory.buildVirtualCurrency(vcObject)
        all[vcCode] = vc
    }

    return VirtualCurrencies(
        all = all,
        jsonObject = this,
    )
}

internal object VirtualCurrenciesResponseJsonKeys {
    const val VIRTUAL_CURRENCIES = "virtual_currencies"
}
