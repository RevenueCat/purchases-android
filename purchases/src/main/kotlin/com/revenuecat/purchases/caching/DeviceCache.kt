//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.caching

import android.content.SharedPreferences
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchaseWrapper
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.buildPurchaserInfo
import com.revenuecat.purchases.debugLog
import com.revenuecat.purchases.sha1

import org.json.JSONException
import org.json.JSONObject
import java.util.Date

private const val CACHE_REFRESH_PERIOD = 60000 * 5

internal class DeviceCache(
    private val preferences: SharedPreferences,
    apiKey: String
) {
    val legacyAppUserIDCacheKey = "com.revenuecat.purchases.$apiKey"
    val appUserIDCacheKey = "com.revenuecat.purchases.$apiKey.new"
    private val attributionCacheKey = "com.revenuecat.purchases.attribution"
    val tokensCacheKey = "com.revenuecat.purchases.$apiKey.tokens"

    private var purchaserInfoCachesLastUpdated: Date? = null

    private var offeringsCachedObject: InMemoryCachedObject<Offerings> = InMemoryCachedObject(CACHE_REFRESH_PERIOD)

    fun purchaserInfoCacheKey(appUserID: String) = "$legacyAppUserIDCacheKey.$appUserID"

    fun getCachedPurchaserInfo(appUserID: String): PurchaserInfo? {
        return preferences.getString(purchaserInfoCacheKey(appUserID), null)
            ?.let { json ->
                try {
                    val cachedJSONObject = JSONObject(json)
                    val schemaVersion = cachedJSONObject.optInt("schema_version")
                    return if (schemaVersion == PurchaserInfo.SCHEMA_VERSION) {
                        cachedJSONObject.buildPurchaserInfo()
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

    @Synchronized
    fun clearCachesForAppUserID() {
        preferences.edit()
            .also { editor ->
                getCachedAppUserID()?.let {
                    editor.remove(purchaserInfoCacheKey(it))
                }
                getLegacyCachedAppUserID()?.let {
                    editor.remove(purchaserInfoCacheKey(it))
                }
            }
            .remove(appUserIDCacheKey)
            .remove(legacyAppUserIDCacheKey)
            .apply()
        clearPurchaserInfoCacheTimestamp()
        offeringsCachedObject.clearCache()
    }

    @Synchronized fun getCachedAppUserID(): String? = preferences.getString(appUserIDCacheKey, null)

    @Synchronized fun getLegacyCachedAppUserID(): String? = preferences.getString(legacyAppUserIDCacheKey, null)

    @Synchronized fun cacheAppUserID(appUserID: String) {
        preferences.edit().putString(appUserIDCacheKey, appUserID).apply()
    }

    @Synchronized fun getCachedAttributionData(network: Purchases.AttributionNetwork, userId: String): String? =
        preferences.getString(getAttributionDataCacheKey(userId, network), null)

    @Synchronized fun cacheAttributionData(network: Purchases.AttributionNetwork, userId: String, cacheValue: String) {
        preferences.edit().putString(getAttributionDataCacheKey(userId, network), cacheValue).apply()
    }

    @Synchronized fun clearLatestAttributionData(userId: String) {
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

    @Synchronized
    fun clearPurchaserInfoCacheTimestamp() {
        purchaserInfoCachesLastUpdated = null
    }

    @Synchronized
    fun clearOfferingsCacheTimestamp() {
        offeringsCachedObject.clearCacheTimestamp()
    }

    private fun getAttributionDataCacheKey(
        userId: String,
        network: Purchases.AttributionNetwork
    ) = "$attributionCacheKey.$userId.$network"

    @Synchronized fun cacheOfferings(offerings: Offerings) {
        offeringsCachedObject.cacheInstance(offerings, Date())
    }

    @Synchronized fun setPurchaserInfoCacheTimestampToNow() {
        purchaserInfoCachesLastUpdated = Date()
    }

    @Synchronized fun setOfferingsCacheTimestampToNow() {
        offeringsCachedObject.updateCacheTimestamp(Date())
    }

    @Synchronized
    fun isPurchaserInfoCacheStale(): Boolean {
        return purchaserInfoCachesLastUpdated?.let { cachesLastUpdated ->
            Date().time - cachesLastUpdated.time >= CACHE_REFRESH_PERIOD
        }?: true
    }

    @Synchronized
    fun isOfferingsCacheStale(): Boolean = offeringsCachedObject.isCacheStale()

}
