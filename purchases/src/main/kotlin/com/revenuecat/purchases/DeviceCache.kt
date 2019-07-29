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
                    val cachedPurchaserInfo = JSONObject(json).buildPurchaserInfo()
                    return if (cachedPurchaserInfo.schemaVersion == PurchaserInfo.SCHEMA_VERSION) {
                        cachedPurchaserInfo
                    } else {
                        null
                    }
                } catch (e: JSONException) {
                    null
                }
            }
    }

    fun cachePurchaserInfo(appUserID: String, info: PurchaserInfo) {
        val jsonObject = info.jsonObject.also {
            it.put("schema_version", PurchaserInfo.SCHEMA_VERSION)
        }
        preferences.edit()
            .putString(
                purchaserInfoCacheKey(appUserID),
                jsonObject.toString()
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

    @Synchronized internal fun getPreviouslySentHashedTokens(): Set<String> {
        return (preferences.getStringSet(tokensCacheKey, emptySet())?.toSet() ?: emptySet()).also {
            debugLog("[QueryPurchases] Tokens already posted: $it")
        }
    }

    @Synchronized fun addSuccessfullyPostedToken(token: String) {
        debugLog("[QueryPurchases] Saving token $token with hash ${token.sha1()}")
        getPreviouslySentHashedTokens().let {
            debugLog("[QueryPurchases] Tokens in cache before saving $it")
            setSavedTokenHashes(it.toMutableSet().apply { add(token.sha1()) })
        }
    }

    @Synchronized private fun setSavedTokenHashes(newSet: Set<String>) {
        debugLog("[QueryPurchases] Saving tokens $newSet")
        preferences.edit().putStringSet(tokensCacheKey, newSet).apply()
    }

    /**
     * Removes from the database all hashed tokens that are not considered active anymore, i.e. all
     * consumed in-apps or inactive subscriptions hashed tokens that are still in the local cache.
     */
    @Synchronized fun cleanPreviouslySentTokens(
        activeSubsHashedTokens: Set<String>,
        unconsumedInAppsHashedTokens: Set<String>
    ) {
        debugLog("[QueryPurchases] Cleaning previously sent tokens")
        setSavedTokenHashes((activeSubsHashedTokens + unconsumedInAppsHashedTokens).intersect(getPreviouslySentHashedTokens()))
    }

    /**
     * Returns a list containing all tokens that are in [activeSubsByTheirHashedToken] and
     * [activeInAppsByTheirHashedToken] map that are not present in the device cache.
     * In other words, returns all hashed tokens that are active and have not
     * been posted to our backend yet.
     */
    @Synchronized fun getActivePurchasesNotInCache(
        activeSubsByTheirHashedToken: Map<String, PurchaseWrapper>,
        activeInAppsByTheirHashedToken: Map<String, PurchaseWrapper>
    ): List<PurchaseWrapper> {
        return activeSubsByTheirHashedToken
            .plus(activeInAppsByTheirHashedToken)
            .minus(getPreviouslySentHashedTokens())
            .values.toList()
    }

    private fun getAttributionDataCacheKey(
        userId: String,
        network: Purchases.AttributionNetwork
    ) = "$attributionCacheKey.$userId.$network"

}
