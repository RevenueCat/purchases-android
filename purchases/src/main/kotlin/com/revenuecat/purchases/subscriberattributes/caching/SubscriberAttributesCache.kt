//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.subscriberattributes.caching

import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.AttributionStrings
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import com.revenuecat.purchases.subscriberattributes.buildSubscriberAttributesMapPerUser

internal typealias AppUserID = String
internal typealias SubscriberAttributeMap = Map<String, SubscriberAttribute>
internal typealias SubscriberAttributesPerAppUserIDMap = Map<AppUserID, SubscriberAttributeMap>

internal class SubscriberAttributesCache(
    internal val deviceCache: DeviceCache,
) {

    internal val subscriberAttributesCacheKey: String by lazy {
        deviceCache.newKey("subscriberAttributes")
    }

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

    @Synchronized
    fun clearAllSubscriberAttributesFromUser(appUserID: AppUserID) {
        log(LogIntent.DEBUG, AttributionStrings.DELETING_ATTRIBUTES.format(appUserID))
        val allStoredSubscriberAttributes = getAllStoredSubscriberAttributes()
        val updatedStoredSubscriberAttributes =
            allStoredSubscriberAttributes.toMutableMap().also {
                it.remove(appUserID)
            }.toMap()
        deviceCache.putAttributes(updatedStoredSubscriberAttributes)
    }

    @Synchronized
    fun clearSubscriberAttributesIfSyncedForSubscriber(appUserID: AppUserID) {
        val unsyncedSubscriberAttributes = getUnsyncedSubscriberAttributes(appUserID)
        if (unsyncedSubscriberAttributes.isEmpty()) {
            clearAllSubscriberAttributesFromUser(appUserID)
        }
    }

    @Synchronized
    fun cleanUpSubscriberAttributeCache(currentAppUserID: String) {
        migrateSubscriberAttributesIfNeeded()
        deleteSyncedSubscriberAttributesForOtherUsers(currentAppUserID)
    }

    internal fun DeviceCache.putAttributes(
        updatedSubscriberAttributesForAll: SubscriberAttributesPerAppUserIDMap,
    ) {
        return deviceCache.putString(
            subscriberAttributesCacheKey,
            updatedSubscriberAttributesForAll.toJSONObject().toString(),
        )
    }

    @Synchronized
    private fun deleteSyncedSubscriberAttributesForOtherUsers(currentAppUserID: String) {
        log(LogIntent.DEBUG, AttributionStrings.DELETING_ATTRIBUTES_OTHER_USERS.format(currentAppUserID))

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

    private fun SubscriberAttributeMap.filterUnsynced(appUserID: AppUserID): SubscriberAttributeMap =
        this.filterValues { !it.isSynced }
            .also { unsyncedAttributesByKey ->
                log(
                    LogIntent.DEBUG,
                    AttributionStrings.UNSYNCED_ATTRIBUTES_COUNT
                        .format(unsyncedAttributesByKey.count(), appUserID) +
                        if (unsyncedAttributesByKey.isNotEmpty()) {
                            unsyncedAttributesByKey.values.joinToString("\n")
                        } else {
                            ""
                        },
                )
            }
}
