package com.revenuecat.purchases.strings

internal object VirtualCurrencyStrings {
    const val INVALIDATING_VIRTUAL_CURRENCIES_CACHE = "Invalidating VirtualCurrencies cache."
    const val VENDING_FROM_CACHE = "Vending VirtualCurrencies from cache."
    const val NO_CACHED_VIRTUAL_CURRENCIES = "There are no cached VirtualCurrencies."
    const val VIRTUAL_CURRENCIES_STALE_UPDATING_FROM_NETWORK = "VirtualCurrencies cache is stale, updating from " +
        "network."
    const val VIRTUAL_CURRENCIES_UPDATED_FROM_NETWORK = "VirtualCurrencies updated from the network."
    const val VIRTUAL_CURRENCIES_UPDATED_FROM_NETWORK_ERROR = "Attempt to update VirtualCurrencies from network " +
        "failed. Error: %s"
    const val EMPTY_SPEND_AMOUNT = "No amount specified to spend. This will be ignored."
    const val INVALID_CODE = "Invalid Virtual Currency code: %s"
    const val INVALID_SPEND_AMOUNT = "Attempt to spend zero/negative currency amount %d for %s. " +
        "This is not allowed and will be ignored."
    const val TOO_MANY_CODES = "You may not specify this many codes in a single spend transaction."
    const val VIRTUAL_CURRENCIES_SPENT_ON_NETWORK = "VirtualCurrencies spent on the network."
    const val VIRTUAL_CURRENCIES_SPENT_ON_NETWORK_ERROR = "Attempt to spend VirtualCurrencies on network " +
        "failed. Error: %s"
    const val SPEND_REQUIRES_IAM = "Spending virtual currencies requires configuring the SDK with iamEnabled(true)."
    const val ERROR_DECODING_CACHED_VIRTUAL_CURRENCIES = "Couldn't decode cached VirtualCurrencies. Error: %s"
}
