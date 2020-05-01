package com.revenuecat.purchases.attributes

import com.revenuecat.purchases.Backend
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.SubscriberAttributeError
import com.revenuecat.purchases.caching.AppUserID
import com.revenuecat.purchases.caching.DeviceCache
import com.revenuecat.purchases.debugLog
import com.revenuecat.purchases.errorLog

internal class SubscriberAttributesManager(
    val deviceCache: DeviceCache,
    val backend: Backend
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
    fun setAttribute(key: SubscriberAttributeKey, value: String?, appUserID: String) {
        setAttributes(mapOf(key.backendKey to value), appUserID)
    }

    fun synchronizeSubscriberAttributesForAllUsers(
        currentAppUserID: AppUserID
    ) {
        val unsyncedStoredAttributesForAllUsers =
            deviceCache.getUnsyncedSubscriberAttributes()
        if (unsyncedStoredAttributesForAllUsers.isEmpty()) {
            debugLog("No subscriber attributes to synchronize.")
            return
        }

        unsyncedStoredAttributesForAllUsers.forEach { (appUserID, unsyncedAttributesForUser) ->
            backend.postSubscriberAttributes(
                unsyncedAttributesForUser,
                appUserID,
                {
                    markAsSynced(appUserID, unsyncedAttributesForUser, emptyList())
                    debugLog("Subscriber attributes synced successfully for appUserID: $appUserID.")
                    if (currentAppUserID != appUserID) {
                        deviceCache.clearSubscriberAttributesIfSynced(appUserID)
                    }
                },
                { error, didBackendGetAttributes, attributeErrors ->
                    if (didBackendGetAttributes) {
                        markAsSynced(appUserID, unsyncedAttributesForUser, attributeErrors)
                    }
                    errorLog("There was an error syncing subscriber attributes for " +
                        "appUserID: $appUserID. Error: $error")
                }
            )
        }
    }

    fun synchronizeSubscriberAttributesIfNeeded(
        appUserID: String,
        onSuccessHandler: () -> Unit,
        onErrorHandler: (PurchasesError) -> Unit
    ) {
        val unsyncedStoredAttributes = deviceCache.getUnsyncedSubscriberAttributes(appUserID)
        if (unsyncedStoredAttributes.isEmpty()) {
            debugLog("No subscriber attributes to synchronize.")
            onSuccessHandler()
            return
        }

        debugLog("Synchronizing subscriber attributes for $appUserID \n " +
            unsyncedStoredAttributes.values.joinToString("\n")
        )
        backend.postSubscriberAttributes(
            unsyncedStoredAttributes,
            appUserID,
            {
                markAsSynced(appUserID, unsyncedStoredAttributes, emptyList())
                onSuccessHandler()
            },
            { error, didBackendGetAttributes, attributeErrors ->
                if (didBackendGetAttributes) {
                    markAsSynced(appUserID, unsyncedStoredAttributes, attributeErrors)
                }
                onErrorHandler(error)
            }
        )
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
            errorLog("There were some subscriber attributes errors: $attributeErrors")
        }
        if (attributesToMarkAsSynced.isEmpty()) {
            return
        }
        debugLog("Marking the following attributes as synced for appUserID: $appUserID: \n" +
            attributesToMarkAsSynced.values.joinToString("\n"))
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
}