package com.revenuecat.purchases.subscriberattributes

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.subscriberattributes.caching.AppUserID
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog

class SubscriberAttributesManager(
    val deviceCache: SubscriberAttributesCache,
    val backend: SubscriberAttributesBackend
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
            debugLog("No subscriber attributes to synchronize.")
            return
        }

        unsyncedStoredAttributesForAllUsers.forEach { (syncingAppUserID, unsyncedAttributesForUser) ->
            backend.postSubscriberAttributes(
                unsyncedAttributesForUser.toBackendMap(),
                syncingAppUserID,
                {
                    markAsSynced(syncingAppUserID, unsyncedAttributesForUser, emptyList())
                    debugLog("Subscriber attributes synced successfully for appUserID: $syncingAppUserID.")
                    if (currentAppUserID != syncingAppUserID) {
                        deviceCache.clearSubscriberAttributesIfSyncedForSubscriber(syncingAppUserID)
                    }
                },
                { error, didBackendGetAttributes, attributeErrors ->
                    if (didBackendGetAttributes) {
                        markAsSynced(syncingAppUserID, unsyncedAttributesForUser, attributeErrors)
                    }
                    errorLog(
                        "There was an error syncing subscriber attributes for " +
                            "appUserID: $syncingAppUserID. Error: $error"
                    )
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
            errorLog("There were some subscriber attributes errors: $attributeErrors")
        }
        if (attributesToMarkAsSynced.isEmpty()) {
            return
        }
        debugLog(
            "Marking the following attributes as synced for appUserID: $appUserID: \n" +
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
     * Collect gpsAdId and androidId automatically
     *
     * This method cannot be called in the main thread as it may block leading to ANRs.
     * An IllegalStateException will be thrown if this is called on the main thread.
     */
    fun collectDeviceIdentifiers(
        appUserID: String,
        applicationContext: Application
    ) {
        val attributesToSet = getDeviceIdentifiers(applicationContext)
        setAttributes(attributesToSet, appUserID)
    }

    private fun getDeviceIdentifiers(applicationContext: Application): MutableMap<String, String?> {
        val deviceIdentifiers = mutableMapOf<String, String?>()
        try {
            val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)
            if (!adInfo.isLimitAdTrackingEnabled) {
                val advertisingID = adInfo.id
                deviceIdentifiers[SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey] = advertisingID
            }
        } catch (e: GooglePlayServicesNotAvailableException) {
            errorLog("GooglePlayServices is not installed. Couldn't get and advertising identifier. " +
                "Message: ${e.localizedMessage}")
        } catch (e: GooglePlayServicesRepairableException) {
            errorLog("GooglePlayServicesRepairableException when getting advertising identifier. " +
                "Message: ${e.localizedMessage}")
        }
        @SuppressLint("HardwareIds")
        val androidID =
            Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        deviceIdentifiers[SubscriberAttributeKey.DeviceIdentifiers.AndroidID.backendKey] = androidID
        return deviceIdentifiers
    }

    /**
     * Set the specific ID for the specified attribution network. It also collects GPS ID and ANDROID_ID automatically.
     *
     * This method cannot be called in the main thread as it may block leading to ANRs.
     * An IllegalStateException will be thrown if this is called on the main thread.
     */
    fun setAttributionID(
        attributionKey: SubscriberAttributeKey.AttributionIds,
        value: String?,
        appUserID: String,
        applicationContext: Application
    ) {
        val deviceIdentifiers = getDeviceIdentifiers(applicationContext)
        val attributesToSet = mapOf(attributionKey.backendKey to value) + deviceIdentifiers
        setAttributes(attributesToSet, appUserID)
    }
}
