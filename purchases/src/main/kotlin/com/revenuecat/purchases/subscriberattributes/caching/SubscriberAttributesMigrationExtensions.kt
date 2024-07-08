package com.revenuecat.purchases.subscriberattributes.caching

import android.content.SharedPreferences
import com.revenuecat.purchases.subscriberattributes.buildLegacySubscriberAttributes

@Synchronized
internal fun SubscriberAttributesCache.migrateSubscriberAttributesIfNeeded(cacheEditor: SharedPreferences.Editor) {
    getAllLegacyStoredSubscriberAttributes()
        .takeIf { it.isNotEmpty() }
        ?.let { legacySubscriberAttributes ->
            migrateSubscriberAttributes(legacySubscriberAttributes, cacheEditor)
        }
}

@Synchronized
internal fun SubscriberAttributesCache.migrateSubscriberAttributes(
    legacySubscriberAttributesForAppUserID: SubscriberAttributesPerAppUserIDMap,
    cacheEditor: SharedPreferences.Editor,
) {
    val storedSubscriberAttributesForAll: SubscriberAttributesPerAppUserIDMap =
        getAllStoredSubscriberAttributes()

    val updatedStoredSubscriberAttributesForAll: MutableMap<String, SubscriberAttributeMap> =
        storedSubscriberAttributesForAll.toMutableMap()

    legacySubscriberAttributesForAppUserID.forEach { (appUserID: AppUserID, legacy: SubscriberAttributeMap) ->
        val current: SubscriberAttributeMap = storedSubscriberAttributesForAll[appUserID] ?: emptyMap()
        val updated: SubscriberAttributeMap = legacy + current
        updatedStoredSubscriberAttributesForAll[appUserID] = updated
        cacheEditor.remove(legacySubscriberAttributesCacheKey(appUserID))
    }

    cacheEditor.putString(
        subscriberAttributesCacheKey,
        updatedStoredSubscriberAttributesForAll.toJSONObject().toString(),
    )
}

internal fun SubscriberAttributesCache.legacySubscriberAttributesCacheKey(appUserID: String) =
    "$subscriberAttributesCacheKey.$appUserID"

@Synchronized
internal fun SubscriberAttributesCache.getAllLegacyStoredSubscriberAttributes(): SubscriberAttributesPerAppUserIDMap {
    val legacySubscriberAttributesCacheKeyPrefix = legacySubscriberAttributesCacheKey("")
    val allSubscriberAttributesKeys = deviceCache.findKeysThatStartWith(legacySubscriberAttributesCacheKeyPrefix)

    return allSubscriberAttributesKeys.associate { preferencesKey ->
        val appUserIDFromKey: AppUserID =
            preferencesKey.split(legacySubscriberAttributesCacheKeyPrefix)[1]
        val subscriberAttributeMap: SubscriberAttributeMap =
            deviceCache.getJSONObjectOrNull(preferencesKey)
                ?.buildLegacySubscriberAttributes() ?: emptyMap()
        appUserIDFromKey to subscriberAttributeMap
    }
}
