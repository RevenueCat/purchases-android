package com.revenuecat.purchases.strings

internal object VirtualCurrencyStrings {
    const val INVALIDATING_VIRTUAL_CURRENCIES_CACHE = "Invalidating VirtualCurrencies cache."
    const val VENDING_FROM_CACHE = "Vending VirtualCurrencies from cache."
    const val NO_CACHED_VIRTUAL_CURRENCIES = "No cached VirtualCurrencies, fetching from network."
    const val VIRTUAL_CURRENCIES_STALE_UPDATING_FROM_NETWORK = "VirtualCurrencies cache is stale, updating from " +
        "network."
    const val VIRTUAL_CURRENCIES_UPDATED_FROM_NETWORK = "VirtualCurrencies updated from the network."
    const val VIRTUAL_CURRENCIES_UPDATED_FROM_NETWORK_ERROR = "Attempt to update VirtualCurrencies from network " +
        "failed. Error: %s"
    const val ERROR_DECODING_CACHED_VIRTUAL_CURRENCIES = "Couldn't decode cached VirtualCurrencies. Error: %s"
}
