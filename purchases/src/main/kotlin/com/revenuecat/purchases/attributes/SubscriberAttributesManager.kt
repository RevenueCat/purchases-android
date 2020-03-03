package com.revenuecat.purchases.attributes

import com.revenuecat.purchases.Backend
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.caching.DeviceCache

internal class SubscriberAttributesManager(
    val deviceCache: DeviceCache,
    val backend: Backend
) {

    @Synchronized
    fun setAttributes(attributes: Map<String, String?>, appUserID: String) {
        val mappedAttributes= attributes.map { (key, value) ->
            key to SubscriberAttribute(key, value)
        }.toMap()

        deviceCache.setAttributes(appUserID, mappedAttributes)
    }

    @Synchronized
    fun setAttribute(key: SubscriberAttributeKey, value: String?, appUserID: String) {
        setAttributes(mapOf(key.serverValue to value), appUserID)
    }

    fun syncronizeSusbcriberAttributes(
        appUserID: String,
        onSuccessHandler: () -> Unit,
        onErrorHandler: (PurchasesError) -> Unit
    ) {
        val unsyncedStoredAttributes =
            deviceCache.getAllStoredSubscriberAttributes(appUserID)
                .filter { (_, attribute) -> !attribute.isSynced }
        backend.postSubscriberAttributes(unsyncedStoredAttributes, appUserID, {
            markAsSynced(appUserID, unsyncedStoredAttributes)
            onSuccessHandler()
        }, { error, errorIsFinishable ->
            if (errorIsFinishable) {
                markAsSynced(appUserID, unsyncedStoredAttributes)
            }
            onErrorHandler(error)
        })
    }

    @Synchronized fun markAsSynced(
        appUserID: String,
        attributesToMarkAsSynced: Map<String, SubscriberAttribute>
    ) {
        if (attributesToMarkAsSynced.isEmpty()) {
            return
        }
        val currentlyStoredAttributes = deviceCache.getAllStoredSubscriberAttributes(appUserID)
        val attributesToBeSet = currentlyStoredAttributes.toMutableMap()
        attributesToMarkAsSynced.forEach { (key, subscriberAttribute) ->
            currentlyStoredAttributes[key]
                ?.takeUnless { it.isSynced }
                ?.takeUnless { it.value == subscriberAttribute.value }
                ?.let { attributesToBeSet[key] = subscriberAttribute.copy(isSynced = true) }
        }

        deviceCache.setAttributes(appUserID, attributesToBeSet)
    }
}