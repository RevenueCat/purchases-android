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
import com.revenuecat.purchases.attributes.SubscriberAttribute
import com.revenuecat.purchases.buildLegacySubscriberAttributes
import com.revenuecat.purchases.buildPurchaserInfo
import com.revenuecat.purchases.buildSubscriberAttributesMapPerUser
import com.revenuecat.purchases.debugLog
import com.revenuecat.purchases.sha1
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

private const val CACHE_REFRESH_PERIOD = 60000 * 5
private const val SHARED_PREFERENCES_PREFIX = "com.revenuecat.purchases."

internal typealias AppUserID = String
internal typealias SubscriberAttributeMap = Map<String, SubscriberAttribute>
internal typealias SubscriberAttributesPerAppUserIDMap = Map<AppUserID, SubscriberAttributeMap>

internal class DeviceCache(
    private val preferences: SharedPreferences,
    private val apiKey: String,
    private val offeringsCachedObject: InMemoryCachedObject<Offerings> = InMemoryCachedObject(
        CACHE_REFRESH_PERIOD
    )
) {
    val legacyAppUserIDCacheKey: String by lazy { "$SHARED_PREFERENCES_PREFIX$apiKey" }
    val appUserIDCacheKey: String by lazy { "$SHARED_PREFERENCES_PREFIX$apiKey.new" }
    val attributionCacheKey = "$SHARED_PREFERENCES_PREFIX.attribution"
    val tokensCacheKey: String by lazy { "$SHARED_PREFERENCES_PREFIX$apiKey.tokens" }
    val subscriberAttributesCacheKey: String by lazy { "$SHARED_PREFERENCES_PREFIX$apiKey.subscriberAttributes" }

    var purchaserInfoCachesLastUpdated: Date? = null

    // region app user id

    @Synchronized
    fun getLegacyCachedAppUserID(): String? = preferences.getString(legacyAppUserIDCacheKey, null)

    @Synchronized
    fun getCachedAppUserID(): String? = preferences.getString(appUserIDCacheKey, null)

    @Synchronized
    fun cacheAppUserID(appUserID: String) {
        preferences.edit().putString(appUserIDCacheKey, appUserID).apply()
    }

    @Synchronized
    fun clearCachesForAppUserID(currentAppUserID: String) {
        preferences.edit()
            .clearPurchaserInfo()
            .clearAppUserID()
            .apply()
        clearPurchaserInfoCacheTimestamp()
        clearOfferingsCache()
        clearSubscriberAttributesIfSynced(currentAppUserID)
    }

    private fun SharedPreferences.Editor.clearPurchaserInfo(): SharedPreferences.Editor {
        getCachedAppUserID()?.let {
            remove(purchaserInfoCacheKey(it))
        }
        getLegacyCachedAppUserID()?.let {
            remove(purchaserInfoCacheKey(it))
        }
        return this
    }

    private fun SharedPreferences.Editor.clearAppUserID(): SharedPreferences.Editor {
        remove(appUserIDCacheKey)
        remove(legacyAppUserIDCacheKey)
        return this
    }

    // endregion

    // region purchaser info
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

    @Synchronized
    fun cachePurchaserInfo(appUserID: String, info: PurchaserInfo) {
        val jsonObject = info.jsonObject.also {
            it.put("schema_version", PurchaserInfo.SCHEMA_VERSION)
        }
        preferences.edit()
            .putString(
                purchaserInfoCacheKey(appUserID),
                jsonObject.toString()
            ).apply()

        setPurchaserInfoCacheTimestampToNow()
    }

    @Synchronized
    fun isPurchaserInfoCacheStale(): Boolean {
        return purchaserInfoCachesLastUpdated?.let { cachesLastUpdated ->
            Date().time - cachesLastUpdated.time >= CACHE_REFRESH_PERIOD
        } ?: true
    }

    @Synchronized
    fun clearPurchaserInfoCacheTimestamp() {
        purchaserInfoCachesLastUpdated = null
    }

    @Synchronized
    fun setPurchaserInfoCacheTimestampToNow() {
        purchaserInfoCachesLastUpdated = Date()
    }

    // endregion

    // region attribution data

    @Synchronized
    fun getCachedAttributionData(network: Purchases.AttributionNetwork, userId: String): String? =
        preferences.getString(getAttributionDataCacheKey(userId, network), null)

    @Synchronized
    fun cacheAttributionData(
        network: Purchases.AttributionNetwork,
        userId: String,
        cacheValue: String
    ) {
        preferences.edit().putString(getAttributionDataCacheKey(userId, network), cacheValue)
            .apply()
    }

    @Synchronized
    fun clearLatestAttributionData(userId: String) {
        val editor = preferences.edit()
        Purchases.AttributionNetwork.values().forEach { network ->
            editor.remove(getAttributionDataCacheKey(userId, network))
        }
        editor.apply()
    }

    // endregion

    // region purchase tokens

    @Synchronized
    internal fun getPreviouslySentHashedTokens(): Set<String> {
        return (preferences.getStringSet(tokensCacheKey, emptySet())?.toSet() ?: emptySet()).also {
            debugLog("[QueryPurchases] Tokens already posted: $it")
        }
    }

    @Synchronized
    fun addSuccessfullyPostedToken(token: String) {
        debugLog("[QueryPurchases] Saving token $token with hash ${token.sha1()}")
        getPreviouslySentHashedTokens().let {
            debugLog("[QueryPurchases] Tokens in cache before saving $it")
            setSavedTokenHashes(it.toMutableSet().apply { add(token.sha1()) })
        }
    }

    @Synchronized
    private fun setSavedTokenHashes(newSet: Set<String>) {
        debugLog("[QueryPurchases] Saving tokens $newSet")
        preferences.edit().putStringSet(tokensCacheKey, newSet).apply()
    }

    /**
     * Removes from the database all hashed tokens that are not considered active anymore, i.e. all
     * consumed in-apps or inactive subscriptions hashed tokens that are still in the local cache.
     */
    @Synchronized
    fun cleanPreviouslySentTokens(
        activeSubsHashedTokens: Set<String>,
        unconsumedInAppsHashedTokens: Set<String>
    ) {
        debugLog("[QueryPurchases] Cleaning previously sent tokens")
        setSavedTokenHashes(
            (activeSubsHashedTokens + unconsumedInAppsHashedTokens).intersect(
                getPreviouslySentHashedTokens()
            )
        )
    }

    /**
     * Returns a list containing all tokens that are in [activeSubsByTheirHashedToken] and
     * [activeInAppsByTheirHashedToken] map that are not present in the device cache.
     * In other words, returns all hashed tokens that are active and have not
     * been posted to our backend yet.
     */
    @Synchronized
    fun getActivePurchasesNotInCache(
        activeSubsByTheirHashedToken: Map<String, PurchaseWrapper>,
        activeInAppsByTheirHashedToken: Map<String, PurchaseWrapper>
    ): List<PurchaseWrapper> {
        return activeSubsByTheirHashedToken
            .plus(activeInAppsByTheirHashedToken)
            .minus(getPreviouslySentHashedTokens())
            .values.toList()
    }

    // endregion

    // region offerings

    val cachedOfferings: Offerings?
        get() = offeringsCachedObject.cachedInstance

    @Synchronized
    fun cacheOfferings(offerings: Offerings) {
        offeringsCachedObject.cacheInstance(offerings)
    }

    @Synchronized
    fun isOfferingsCacheStale(): Boolean = offeringsCachedObject.isCacheStale()

    @Synchronized
    fun clearOfferingsCacheTimestamp() {
        offeringsCachedObject.clearCacheTimestamp()
    }

    @Synchronized
    fun setOfferingsCacheTimestampToNow() {
        offeringsCachedObject.updateCacheTimestamp(Date())
    }

    // endregion

    // region subscriber attributes

    @Synchronized
    fun setAttributes(appUserID: AppUserID, attributesToBeSet: SubscriberAttributeMap) {
        val currentlyStoredAttributesForAllUsers = getAllStoredSubscriberAttributes()
        val currentlyStoredAttributesForAppUserID = currentlyStoredAttributesForAllUsers[appUserID] ?: emptyMap()
        val updatedAttributesForUser = currentlyStoredAttributesForAppUserID + attributesToBeSet
        val updatedAttributesForAllUsers = currentlyStoredAttributesForAllUsers + mapOf(appUserID to updatedAttributesForUser)

        preferences.edit().putAttributes(updatedAttributesForAllUsers).apply()
    }

    @Synchronized
    fun getAllStoredSubscriberAttributes(): SubscriberAttributesPerAppUserIDMap =
        preferences.getJSONObjectOrNull(subscriberAttributesCacheKey)
            ?.buildSubscriberAttributesMapPerUser() ?: emptyMap()

    @Synchronized
    fun getAllStoredSubscriberAttributes(appUserID: AppUserID): SubscriberAttributeMap =
        getAllStoredSubscriberAttributes()[appUserID] ?: emptyMap()

    @Synchronized
    fun getUnsyncedSubscriberAttributes(): SubscriberAttributesPerAppUserIDMap =
        getAllStoredSubscriberAttributes()
            .mapValues { (appUserID, attributesForUser) ->
                attributesForUser.filterUnsynced(appUserID)
            }.filterValues { it.isNotEmpty() }

    @Synchronized
    fun getUnsyncedSubscriberAttributes(appUserID: String) =
        getAllStoredSubscriberAttributes(appUserID).filterUnsynced(appUserID)

    private fun SubscriberAttributeMap.filterUnsynced(appUserID: AppUserID): SubscriberAttributeMap =
        this.filterValues { !it.isSynced }
            .also { unsyncedAttributesByKey ->
                debugLog("Found ${unsyncedAttributesByKey.count()} unsynced attributes for appUserID: $appUserID \n" +
                    if (unsyncedAttributesByKey.isNotEmpty()) {
                        "\t ${unsyncedAttributesByKey.map { "${it.value}" }} \n"
                    } else {
                        ""
                    }
                )

            }

    @Synchronized
    fun clearSubscriberAttributesIfSynced(appUserID: String) {
        debugLog("Deleting subscriber attributes for $appUserID from cache.")
        val unsyncedSubscriberAttributes = getUnsyncedSubscriberAttributes(appUserID)
        if (unsyncedSubscriberAttributes.isNotEmpty()) {
            return
        }
        val allStoredSubscriberAttributes = getAllStoredSubscriberAttributes()
        val updatedStoredSubscriberAttributes =
            allStoredSubscriberAttributes.toMutableMap().also {
                it.remove(appUserID)
            }.toMap()

        preferences.edit().putAttributes(updatedStoredSubscriberAttributes).apply()
    }

    private fun SharedPreferences.Editor.putAttributes(
        updatedSubscriberAttributesForAll: SubscriberAttributesPerAppUserIDMap
    ): SharedPreferences.Editor {
        return this.putString(
            subscriberAttributesCacheKey,
            updatedSubscriberAttributesForAll.toJSONObject().toString()
        )
    }

    private fun Map<AppUserID, SubscriberAttributeMap>.toJSONObject(): JSONObject {
        val attributesObject = JSONObject().also { attributesJSONObject ->
            this.forEach { (appUserID, subscriberAttributeMap) ->
                val jsonObject = JSONObject().also { userJSONObject ->
                    subscriberAttributeMap.forEach { (key, subscriberAttribute) ->
                        userJSONObject.put(key, subscriberAttribute.toJSONObject())
                    }
                }
                attributesJSONObject.put(appUserID, jsonObject)
            }
        }
        return JSONObject().also { it.put("attributes", attributesObject) }
    }

    // endregion

    // region migrate subscriber attributes

    @Synchronized
    fun migrateSubscriberAttributesIfNeeded(appUserID: AppUserID) {
        getAllLegacyStoredSubscriberAttributes(appUserID)
            .takeIf { it.isNotEmpty() }
            ?.let { legacySubscriberAttributes -> migrateSubscriberAttributes(legacySubscriberAttributes, appUserID) }
    }

    @Synchronized
    private fun migrateSubscriberAttributes(
        legacySubscriberAttributesForAppUserID: SubscriberAttributeMap,
        appUserID: AppUserID
    ) {
        val storedSubscriberAttributesForAll: SubscriberAttributesPerAppUserIDMap =
            getAllStoredSubscriberAttributes()
        val currentSubscriberAttributesForAppUserID: SubscriberAttributeMap =
            storedSubscriberAttributesForAll[appUserID] ?: emptyMap()

        val updatedAttributesForAppUserIDMap: SubscriberAttributeMap =
            legacySubscriberAttributesForAppUserID + currentSubscriberAttributesForAppUserID
        val updatedStoredSubscriberAttributesForAll: SubscriberAttributesPerAppUserIDMap =
            storedSubscriberAttributesForAll.toMutableMap().also {
                it[appUserID] = updatedAttributesForAppUserIDMap
            }.toMap()
        preferences.edit()
            .putAttributes(updatedStoredSubscriberAttributesForAll)
            .remove(legacySubscriberAttributesCacheKey(appUserID))
            .apply()
    }

    // endregion

    // region legacy subscriber attributes

    fun legacySubscriberAttributesCacheKey(appUserID: String) =
        "$subscriberAttributesCacheKey.$appUserID"

    @Synchronized
    fun getAllLegacyStoredSubscriberAttributes(appUserID: String): SubscriberAttributeMap =
        preferences.getString(legacySubscriberAttributesCacheKey(appUserID), null)
            ?.let { json ->
                try {
                    JSONObject(json)
                } catch (e: JSONException) {
                    null
                }
            }?.buildLegacySubscriberAttributes() ?: emptyMap()

    // endregion

    private fun clearOfferingsCache() {
        offeringsCachedObject.clearCache()
    }

    private fun getAttributionDataCacheKey(
        userId: String,
        network: Purchases.AttributionNetwork
    ) = "$attributionCacheKey.$userId.$network"

    // region utils

    private fun SharedPreferences.getJSONObjectOrNull(key: String): JSONObject? {
        return this.getString(key, null)?.let { json ->
            try {
                JSONObject(json)
            } catch (e: JSONException) {
                null
            }
        }
    }

    // endregion

}



