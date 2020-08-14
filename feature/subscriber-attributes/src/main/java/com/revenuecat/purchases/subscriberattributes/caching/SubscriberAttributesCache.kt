//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common.caching

import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import com.revenuecat.purchases.subscriberattributes.buildLegacySubscriberAttributes
import com.revenuecat.purchases.subscriberattributes.buildSubscriberAttributesMapPerUser
import org.json.JSONObject

private const val SHARED_PREFERENCES_PREFIX = "com.revenuecat.purchases."

internal typealias AppUserID = String
internal typealias SubscriberAttributeMap = Map<String, SubscriberAttribute>
internal typealias SubscriberAttributesPerAppUserIDMap = Map<AppUserID, SubscriberAttributeMap>

class SubscriberAttributesCache(
    private val deviceCache: DeviceCache
) {
    private val subscriberAttributesCacheKey: String by lazy {
        deviceCache.newKey("subscriberAttributes")
    }

    // region subscriber attributes

    @Synchronized
    fun setAttributes(appUserID: AppUserID, attributesToBeSet: SubscriberAttributeMap) {
        val currentlyStoredAttributesForAllUsers = getAllStoredSubscriberAttributes()
        val currentlyStoredAttributesForAppUserID =
            currentlyStoredAttributesForAllUsers[appUserID] ?: emptyMap()
        val updatedAttributesForUser = currentlyStoredAttributesForAppUserID + attributesToBeSet
        val updatedAttributesForAllUsers =
            currentlyStoredAttributesForAllUsers + mapOf(appUserID to updatedAttributesForUser)

        deviceCache.putAttributes(updatedAttributesForAllUsers)
    }

    @Synchronized
    fun getAllStoredSubscriberAttributes(): SubscriberAttributesPerAppUserIDMap =
        deviceCache.getJSONObjectOrNull(subscriberAttributesCacheKey)
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
                        unsyncedAttributesByKey.values.joinToString("\n")
                    } else {
                        ""
                    }
                )
            }

    @Synchronized
    fun clearSubscriberAttributesIfSyncedForSubscriber(appUserID: String) {
        val unsyncedSubscriberAttributes = getUnsyncedSubscriberAttributes(appUserID)
        if (unsyncedSubscriberAttributes.isNotEmpty()) {
            return
        }
        debugLog("Deleting subscriber attributes for $appUserID from cache.")
        val allStoredSubscriberAttributes = getAllStoredSubscriberAttributes()
        val updatedStoredSubscriberAttributes =
            allStoredSubscriberAttributes.toMutableMap().also {
                it.remove(appUserID)
            }.toMap()

        deviceCache.putAttributes(updatedStoredSubscriberAttributes)
    }

    @Synchronized
    fun cleanUpSubscriberAttributeCache(currentAppUserID: String) {
        migrateSubscriberAttributesIfNeeded()
        deleteSyncedSubscriberAttributesForOtherUsers(currentAppUserID)
    }

    @Synchronized
    private fun deleteSyncedSubscriberAttributesForOtherUsers(currentAppUserID: String) {
        debugLog("Deleting old synced subscriber attributes that don't belong to $currentAppUserID.")

        val allStoredSubscriberAttributes = getAllStoredSubscriberAttributes()

        val filteredMap =
            allStoredSubscriberAttributes.map { (appUserID, attributesMap) ->
                if (currentAppUserID != appUserID) {
                    appUserID to attributesMap.filterValues { !it.isSynced }
                } else {
                    appUserID to attributesMap
                }
            }.toMap().filterValues { it.isNotEmpty() }

        deviceCache.putAttributes(filteredMap)
    }

    private fun DeviceCache.putAttributes(
        updatedSubscriberAttributesForAll: SubscriberAttributesPerAppUserIDMap
    ) {
        return deviceCache.putString(
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
    private fun migrateSubscriberAttributesIfNeeded() {
        getAllLegacyStoredSubscriberAttributes()
            .takeIf { it.isNotEmpty() }
            ?.let { legacySubscriberAttributes ->
                migrateSubscriberAttributes(legacySubscriberAttributes)
            }
    }

    @Synchronized
    private fun migrateSubscriberAttributes(
        legacySubscriberAttributesForAppUserID: SubscriberAttributesPerAppUserIDMap
    ) {
        val storedSubscriberAttributesForAll: SubscriberAttributesPerAppUserIDMap =
            getAllStoredSubscriberAttributes()

        val updatedStoredSubscriberAttributesForAll: MutableMap<String, SubscriberAttributeMap> =
            storedSubscriberAttributesForAll.toMutableMap()

        legacySubscriberAttributesForAppUserID.forEach { (appUserID: AppUserID, legacy: SubscriberAttributeMap) ->
            val current: SubscriberAttributeMap = storedSubscriberAttributesForAll[appUserID] ?: emptyMap()
            val updated: SubscriberAttributeMap = legacy + current
            updatedStoredSubscriberAttributesForAll[appUserID] = updated
            deviceCache.remove(legacySubscriberAttributesCacheKey(appUserID))
        }

        deviceCache.putAttributes(updatedStoredSubscriberAttributesForAll)
    }

    // endregion

    // region legacy subscriber attributes

    private fun legacySubscriberAttributesCacheKey(appUserID: String) =
        "$subscriberAttributesCacheKey.$appUserID"

    @Synchronized
    private fun getAllLegacyStoredSubscriberAttributes(): SubscriberAttributesPerAppUserIDMap {
        val legacySubscriberAttributesCacheKeyPrefix = legacySubscriberAttributesCacheKey("")
        val allSubscriberAttributesKeys= deviceCache.findKeysThatStartWith(legacySubscriberAttributesCacheKeyPrefix)

        return allSubscriberAttributesKeys.map { preferencesKey ->
            val appUserIDFromKey: AppUserID =
                preferencesKey.split(legacySubscriberAttributesCacheKeyPrefix)[1]
            val subscriberAttributeMap: SubscriberAttributeMap =
                deviceCache.getJSONObjectOrNull(preferencesKey)
                    ?.buildLegacySubscriberAttributes() ?: emptyMap()
            appUserIDFromKey to subscriberAttributeMap
        }.toMap()
    }

    // endregion
}
