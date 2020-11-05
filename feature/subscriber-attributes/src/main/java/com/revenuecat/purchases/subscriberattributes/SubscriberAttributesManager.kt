package com.revenuecat.purchases.subscriberattributes

import android.app.Application
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.strings.AttributionStrings
import com.revenuecat.purchases.subscriberattributes.caching.AppUserID
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache

class SubscriberAttributesManager(
    val deviceCache: SubscriberAttributesCache,
    val backend: SubscriberAttributesPoster,
    private val attributionFetcher: AttributionFetcher
) {

    @Synchronized
    fun setAttributes(attributesToSet: Map<String, String?>, appUserID: String) {
        val attributesAsObjects = attributesToSet.map { (key, value) ->
            key to SubscriberAttribute(key, value)
        }.toMap()

        storeAttributesIfNeeded(attributesAsObjects, appUserID)
    }

    private fun storeAttributesIfNeeded(
        attributesAsObjects: Map<String, SubscriberAttribute>,
        appUserID: String
    ) {
        val currentlyStoredAttributes = deviceCache.getAllStoredSubscriberAttributes(appUserID)
        val attributesToUpdate = attributesAsObjects.filter { (key, attribute) ->
            !currentlyStoredAttributes.containsKey(key) || currentlyStoredAttributes[key]?.value != attribute.value
        }

        if (attributesToUpdate.isNotEmpty()) {
            deviceCache.setAttributes(appUserID, attributesToUpdate)
        }
    }

    @Synchronized
    fun setAttribute(
        key: SubscriberAttributeKey,
        value: String?,
        appUserID: String
    ) {
        setAttributes(mapOf(key.backendKey to value), appUserID)
    }

    fun synchronizeSubscriberAttributesForAllUsers(
        currentAppUserID: AppUserID
    ) {
        val unsyncedStoredAttributesForAllUsers =
            deviceCache.getUnsyncedSubscriberAttributes()
        if (unsyncedStoredAttributesForAllUsers.isEmpty()) {
            debugLog(AttributionStrings.NO_SUBSCRIBER_ATTRIBUTES_TO_SYNCHRONIZE)
            return
        }

        unsyncedStoredAttributesForAllUsers.forEach { (syncingAppUserID, unsyncedAttributesForUser) ->
            backend.postSubscriberAttributes(
                unsyncedAttributesForUser.toBackendMap(),
                syncingAppUserID,
                {
                    markAsSynced(syncingAppUserID, unsyncedAttributesForUser, emptyList())
                    debugLog(AttributionStrings.ATTRIBUTES_SYNC_SUCCESS.format(syncingAppUserID))
                    if (currentAppUserID != syncingAppUserID) {
                        deviceCache.clearSubscriberAttributesIfSyncedForSubscriber(syncingAppUserID)
                    }
                },
                { error, didBackendGetAttributes, attributeErrors ->
                    if (didBackendGetAttributes) {
                        markAsSynced(syncingAppUserID, unsyncedAttributesForUser, attributeErrors)
                    }
                    errorLog(AttributionStrings.SYNCING_ATTRIBUTES_ERROR.format(syncingAppUserID, error))
                }
            )
        }
    }

    @Synchronized
    fun getUnsyncedSubscriberAttributes(appUserID: String) =
        deviceCache.getUnsyncedSubscriberAttributes(appUserID)

    @Synchronized
    fun markAsSynced(
        appUserID: String,
        attributesToMarkAsSynced: Map<String, SubscriberAttribute>,
        attributeErrors: List<SubscriberAttributeError>
    ) {
        if (attributeErrors.isNotEmpty()) {
            errorLog(AttributionStrings.ATTRIBUTE_ERROR.format(attributeErrors))
        }
        if (attributesToMarkAsSynced.isEmpty()) {
            return
        }
        debugLog(AttributionStrings.MARK_ATTRIBUTES_SYNC_SUCCESS.format(appUserID) +
                attributesToMarkAsSynced.values.joinToString("\n")
        )
        val currentlyStoredAttributes = deviceCache.getAllStoredSubscriberAttributes(appUserID)
        val attributesToBeSet = currentlyStoredAttributes.toMutableMap()
        attributesToMarkAsSynced.forEach { (key, subscriberAttribute) ->
            currentlyStoredAttributes[key]
                ?.takeUnless { it.isSynced }
                ?.takeIf { it.value == subscriberAttribute.value }
                ?.let {
                    attributesToBeSet[key] = subscriberAttribute.copy(isSynced = true)
                }
        }

        deviceCache.setAttributes(appUserID, attributesToBeSet)
    }

    /**
     * Collect GPS ID, ANDROID ID and sets IP to true automatically
     */
    fun collectDeviceIdentifiers(
        appUserID: String,
        applicationContext: Application
    ) {
        getDeviceIdentifiers(applicationContext) { deviceIdentifiers ->
            setAttributes(deviceIdentifiers, appUserID)
        }
    }

    /**
     * Set the specific ID for the specified attribution network. It also collects GPS ID, ANDROID ID and sets
     * IP to true automatically.
     */
    fun setAttributionID(
        attributionKey: SubscriberAttributeKey.AttributionIds,
        value: String?,
        appUserID: String,
        applicationContext: Application
    ) {
        getDeviceIdentifiers(applicationContext) { deviceIdentifiers ->
            val attributesToSet = mapOf(attributionKey.backendKey to value) + deviceIdentifiers
            setAttributes(attributesToSet, appUserID)
        }
    }

    private fun getDeviceIdentifiers(
        applicationContext: Application,
        completion: (deviceIdentifiers: Map<String, String?>) -> Unit
    ) {
        attributionFetcher.getDeviceIdentifiers(applicationContext) { advertisingID, androidID ->
            val deviceIdentifiers = mapOf(
                SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey to advertisingID,
                SubscriberAttributeKey.DeviceIdentifiers.AndroidID.backendKey to androidID,
                SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey to "true"
            ).filterValues { it != null }
            completion(deviceIdentifiers)
        }
    }
}
