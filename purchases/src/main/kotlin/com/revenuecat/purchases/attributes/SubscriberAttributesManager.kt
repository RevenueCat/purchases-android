package com.revenuecat.purchases.attributes

import com.revenuecat.purchases.Backend
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.SubscriberAttributeError
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

        deviceCache.setAttributes(appUserID, attributesAsObjects)
    }

    @Synchronized
    fun setAttribute(key: SubscriberAttributeKey, value: String?, appUserID: String) {
        setAttributes(mapOf(key.backendKey to value), appUserID)
    }

    fun synchronizeSubscriberAttributesIfNeeded(
        appUserID: String,
        onSuccessHandler: () -> Unit,
        onErrorHandler: (PurchasesError) -> Unit
    ) {
        val unsyncedStoredAttributes = getUnsyncedSubscriberAttributes(appUserID)

        if (unsyncedStoredAttributes.isNotEmpty()) {
            debugLog("Synchronizing subscriber attributes: $unsyncedStoredAttributes")
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
        } else {
            debugLog("No subscriber attributes to synchronize.")
            onSuccessHandler()
        }
    }

    @Synchronized
    fun getUnsyncedSubscriberAttributes(appUserID: String) =
        deviceCache.getAllStoredSubscriberAttributes(appUserID)
            .filter { (_, attribute) -> !attribute.isSynced }

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