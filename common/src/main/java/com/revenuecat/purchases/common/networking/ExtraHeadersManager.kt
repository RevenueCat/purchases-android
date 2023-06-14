package com.revenuecat.purchases.common.networking

class ExtraHeadersManager {
    private val _extraHeaders = mutableMapOf<String, String>()

    val extraHeaders: Map<String, String>
        get() = _extraHeaders

    fun setBackwardsCompatibleOverride(enabled: Boolean) {
        if (enabled) {
            _extraHeaders["X-RC-BW-Only-Override"] = "true"
        } else {
            _extraHeaders.remove("X-RC-BW-Override")
        }
    }
}
