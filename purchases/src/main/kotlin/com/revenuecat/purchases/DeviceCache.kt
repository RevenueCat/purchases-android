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
    private val attributionCacheKey = "com.revenuecat.purchases.attribution"
    private val tokensCacheKey = "com.revenuecat.purchases.$apiKey.tokens"

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

    fun getCachedAttributionData(network: Purchases.AttributionNetwork, userId: String): String? =
        preferences.getString(getAttributionDataCacheKey(userId, network), null)

    fun cacheAttributionData(network: Purchases.AttributionNetwork, userId: String, cacheValue: String) {
        preferences.edit().putString(getAttributionDataCacheKey(userId, network), cacheValue).apply()
    }

    fun clearLatestAttributionData(userId: String) {
        val editor = preferences.edit()
        Purchases.AttributionNetwork.values().forEach { network ->
            editor.remove(getAttributionDataCacheKey(userId, network))
        }
        editor.apply()
    }

    @Synchronized fun getSentTokens(): Set<String> = preferences.getStringSet(tokensCacheKey, emptySet())!!.toSet()

    @Synchronized fun addSuccessfullyPostedToken(token: String) {
        debugLog("Saving token $token with hash ${token.sha1()}")
        debugLog("Already sent tokens ${getSentTokens()}")
        setSavedTokens(getSentTokens().toMutableSet().apply { add(token.sha1()) })
    }


    @Synchronized fun setSavedTokens(newSet: Set<String>) =
        preferences.edit().putStringSet(tokensCacheKey, newSet).apply()

    private fun getAttributionDataCacheKey(
        userId: String,
        network: Purchases.AttributionNetwork
    ) = "$attributionCacheKey.$userId.$network"
}
