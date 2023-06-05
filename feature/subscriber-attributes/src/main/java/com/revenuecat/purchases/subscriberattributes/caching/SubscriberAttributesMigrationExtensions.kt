package com.revenuecat.purchases.subscriberattributes.caching

import com.revenuecat.purchases.subscriberattributes.buildLegacySubscriberAttributes

@Synchronized
internal fun SubscriberAttributesCache.migrateSubscriberAttributesIfNeeded() {
    getAllLegacyStoredSubscriberAttributes()
        .takeIf { it.isNotEmpty() }
        ?.let { legacySubscriberAttributes ->
            migrateSubscriberAttributes(legacySubscriberAttributes)
        }
}

@Synchronized
internal fun SubscriberAttributesCache.migrateSubscriberAttributes(
    legacySubscriberAttributesForAppUserID: SubscriberAttributesPerAppUserIDMap,
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

internal fun SubscriberAttributesCache.legacySubscriberAttributesCacheKey(appUserID: String) =
    "$subscriberAttributesCacheKey.$appUserID"

@Synchronized
internal fun SubscriberAttributesCache.getAllLegacyStoredSubscriberAttributes(): SubscriberAttributesPerAppUserIDMap {
    val legacySubscriberAttributesCacheKeyPrefix = legacySubscriberAttributesCacheKey("")
    val allSubscriberAttributesKeys = deviceCache.findKeysThatStartWith(legacySubscriberAttributesCacheKeyPrefix)

    return allSubscriberAttributesKeys.map { preferencesKey ->
        val appUserIDFromKey: AppUserID =
            preferencesKey.split(legacySubscriberAttributesCacheKeyPrefix)[1]
        val subscriberAttributeMap: SubscriberAttributeMap =
            deviceCache.getJSONObjectOrNull(preferencesKey)
                ?.buildLegacySubscriberAttributes() ?: emptyMap()
        appUserIDFromKey to subscriberAttributeMap
    }.toMap()
}
