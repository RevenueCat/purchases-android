package com.revenuecat.purchases.amazon.helpers

import android.content.SharedPreferences
import com.revenuecat.purchases.common.caching.DeviceCache
import org.json.JSONObject

internal class MockDeviceCache(
    preferences: SharedPreferences,
    apiKey: String
) : DeviceCache(preferences, apiKey) {

    var stubCache = mutableMapOf<String, String>()

    override fun getJSONObjectOrNull(key: String): JSONObject? {
        return stubCache[key]?.let {
            JSONObject(it)
        }
    }

    override fun putString(
        cacheKey: String,
        value: String
    ) {
        stubCache[cacheKey] = value
    }
}
