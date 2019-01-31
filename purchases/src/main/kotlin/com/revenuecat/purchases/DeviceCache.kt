//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.content.SharedPreferences

import org.json.JSONException
import org.json.JSONObject

internal class DeviceCache(
    private val preferences: SharedPreferences,
    apiKey: String
) {
    private val appUserIDCacheKey = "com.revenuecat.purchases.$apiKey"

    private fun purchaserInfoCacheKey(appUserID: String) = "$appUserIDCacheKey.$appUserID"

    fun getCachedPurchaserInfo(appUserID: String): PurchaserInfo? {
        return preferences.getString(purchaserInfoCacheKey(appUserID), null)
            ?.let { json ->
                try {
                    JSONObject(json).buildPurchaserInfo()
                } catch (e: JSONException) {
                    null
                }
            }
    }

    fun cachePurchaserInfo(appUserID: String, info: PurchaserInfo) {
        preferences.edit()
            .putString(
                purchaserInfoCacheKey(appUserID),
                info.jsonObject.toString()
            ).apply()
    }

    fun clearCachedPurchaserInfo(appUserID: String) {
        preferences.edit()
            .remove(purchaserInfoCacheKey(appUserID))
            .apply()
    }

    fun getCachedAppUserID(): String? = preferences.getString(appUserIDCacheKey, null)

    fun cacheAppUserID(appUserID: String) {
        preferences.edit()
            .putString(
                appUserIDCacheKey,
                appUserID
            ).apply()
    }

}
