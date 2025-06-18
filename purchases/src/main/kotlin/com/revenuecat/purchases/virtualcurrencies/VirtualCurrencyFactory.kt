package com.revenuecat.purchases.virtualcurrencies

import org.json.JSONObject

internal fun JSONObject.buildVirtualCurrency(): VirtualCurrency {
    val balance = getInt(VirtualCurrencyJsonKeys.BALANCE)
    val name = getString(VirtualCurrencyJsonKeys.NAME)
    val code = getString(VirtualCurrencyJsonKeys.CODE)
    val serverDescription = optString(VirtualCurrencyJsonKeys.SERVER_DESCRIPTION)

    return VirtualCurrency(
        balance = balance,
        name = name,
        code = code,
        serverDescription = serverDescription
    )
}

internal object VirtualCurrencyJsonKeys {
    const val BALANCE = "balance"
    const val NAME = "name"
    const val CODE = "code"
    const val SERVER_DESCRIPTION = "description"
}