package com.revenuecat.purchases.subscriberattributes

import android.app.Application
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.subscriberattributes.DeviceIdentifiersFetcher
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.strings.AttributionStrings
import com.revenuecat.purchases.subscriberattributes.caching.AppUserID
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache

class SubscriberAttributesManager(
    val deviceCache: SubscriberAttributesCache,
    val backend: SubscriberAttributesPoster,
    private val deviceIdentifiersFetcher: DeviceIdentifiersFetcher
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
        currentAppUserID: AppUserID,
        completion: (() -> Unit)? = null
    ) {
        val unsyncedStoredAttributesForAllUsers =
            deviceCache.getUnsyncedSubscriberAttributes()
        if (unsyncedStoredAttributesForAllUsers.isEmpty()) {
            log(LogIntent.DEBUG, AttributionStrings.NO_SUBSCRIBER_ATTRIBUTES_TO_SYNCHRONIZE)
            if (completion != null) {
                completion()
            }
            return
        }

        val unsyncedStoredAttributesCount = unsyncedStoredAttributesForAllUsers.size
        var currentSyncedAttributeCount = 0

        unsyncedStoredAttributesForAllUsers.forEach { (syncingAppUserID, unsyncedAttributesForUser) ->
            backend.postSubscriberAttributes(
                unsyncedAttributesForUser.toBackendMap(),
                syncingAppUserID,
                {
                    markAsSynced(syncingAppUserID, unsyncedAttributesForUser, emptyList())
                    log(LogIntent.RC_SUCCESS, AttributionStrings.ATTRIBUTES_SYNC_SUCCESS.format(syncingAppUserID))
                    if (currentAppUserID != syncingAppUserID) {
                        deviceCache.clearSubscriberAttributesIfSyncedForSubscriber(syncingAppUserID)
                    }
                    currentSyncedAttributeCount++
                    if (completion != null && currentSyncedAttributeCount == unsyncedStoredAttributesCount) {
                        completion()
                    }
                },
                { error, didBackendGetAttributes, attributeErrors ->
                    if (didBackendGetAttributes) {
                        markAsSynced(syncingAppUserID, unsyncedAttributesForUser, attributeErrors)
                    }
                    log(LogIntent.RC_ERROR, AttributionStrings.ATTRIBUTES_SYNC_ERROR.format(syncingAppUserID, error))
                    currentSyncedAttributeCount++
                    if (completion != null && currentSyncedAttributeCount == unsyncedStoredAttributesCount) {
                        completion()
                    }
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
            log(LogIntent.RC_ERROR, AttributionStrings.SUBSCRIBER_ATTRIBUTES_ERROR.format(attributeErrors))
        }
        if (attributesToMarkAsSynced.isEmpty()) {
            return
        }
        log(
            LogIntent.INFO, AttributionStrings.MARKING_ATTRIBUTES_SYNCED.format(appUserID) +
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
        setAttribute(attributionKey, value, appUserID)
        collectDeviceIdentifiers(appUserID, applicationContext)
    }

    private fun getDeviceIdentifiers(
        applicationContext: Application,
        completion: (deviceIdentifiers: Map<String, String?>) -> Unit
    ) {
        deviceIdentifiersFetcher.getDeviceIdentifiers(applicationContext) { deviceIdentifiers ->
            completion(deviceIdentifiers)
        }
    }
}
