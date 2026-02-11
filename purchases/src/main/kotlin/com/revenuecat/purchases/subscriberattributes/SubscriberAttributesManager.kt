package com.revenuecat.purchases.subscriberattributes

import android.app.Application
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.infoLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.subscriberattributes.DeviceIdentifiersFetcher
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.strings.AttributionStrings
import com.revenuecat.purchases.subscriberattributes.caching.AppUserID
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributeMap
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.utils.getStringValueForPrimitive
import java.util.Observable

@Suppress("TooManyFunctions")
internal class SubscriberAttributesManager(
    val deviceCache: SubscriberAttributesCache,
    val backend: SubscriberAttributesPoster,
    private val deviceIdentifiersFetcher: DeviceIdentifiersFetcher,
    private val automaticDeviceIdentifierCollectionEnabled: Boolean,
) {

    private val obtainingDeviceIdentifiersObservable = ObtainDeviceIdentifiersObservable()

    @Synchronized
    public fun setAttributes(attributesToSet: Map<String, String?>, appUserID: String) {
        val attributesAsObjects = attributesToSet.map { (key, value) ->
            key to SubscriberAttribute(key, value)
        }.toMap()

        storeAttributesIfNeeded(attributesAsObjects, appUserID)
    }

    private fun storeAttributesIfNeeded(
        attributesAsObjects: Map<String, SubscriberAttribute>,
        appUserID: String,
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
        appUserID: String,
    ) {
        setAttributes(mapOf(key.backendKey to value), appUserID)
    }

    fun synchronizeSubscriberAttributesForAllUsers(
        currentAppUserID: AppUserID,
        completion: (() -> Unit)? = null,
    ) {
        obtainingDeviceIdentifiersObservable.waitUntilIdle {
            // We are filtering out blank user IDs to skip requests for attributes that might have been stored
            // as blank user IDs on older versions of the SDK.
            // See https://github.com/RevenueCat/purchases-android/pull/755
            val unsyncedStoredAttributesForAllUsers =
                deviceCache.getUnsyncedSubscriberAttributes().filterKeys { it.isNotBlank() }
            if (unsyncedStoredAttributesForAllUsers.isEmpty()) {
                log(LogIntent.DEBUG) { AttributionStrings.NO_SUBSCRIBER_ATTRIBUTES_TO_SYNCHRONIZE }
                if (completion != null) {
                    completion()
                }
                return@waitUntilIdle
            }

            val unsyncedStoredAttributesCount = unsyncedStoredAttributesForAllUsers.size
            var currentSyncedAttributeCount = 0

            unsyncedStoredAttributesForAllUsers.forEach { (syncingAppUserID, unsyncedAttributesForUser) ->
                backend.postSubscriberAttributes(
                    unsyncedAttributesForUser.toBackendMap(),
                    syncingAppUserID,
                    {
                        markAsSynced(syncingAppUserID, unsyncedAttributesForUser, emptyList())
                        log(LogIntent.RC_SUCCESS) {
                            AttributionStrings.ATTRIBUTES_SYNC_SUCCESS.format(syncingAppUserID)
                        }
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
                        log(LogIntent.RC_ERROR) {
                            AttributionStrings.ATTRIBUTES_SYNC_ERROR.format(syncingAppUserID, error)
                        }
                        currentSyncedAttributeCount++
                        if (completion != null && currentSyncedAttributeCount == unsyncedStoredAttributesCount) {
                            completion()
                        }
                    },
                )
            }
        }
    }

    @Synchronized
    public fun copyUnsyncedSubscriberAttributes(originalAppUserId: AppUserID, newAppUserID: AppUserID) {
        val unsyncedAttributesPreviousUser = deviceCache.getUnsyncedSubscriberAttributes(originalAppUserId)
        if (unsyncedAttributesPreviousUser.isEmpty()) {
            return
        }
        infoLog { AttributionStrings.COPYING_ATTRIBUTES_FROM_TO_USER.format(originalAppUserId, newAppUserID) }
        deviceCache.setAttributes(newAppUserID, unsyncedAttributesPreviousUser)
        deviceCache.clearAllSubscriberAttributesFromUser(originalAppUserId)
    }

    @Synchronized
    public fun getUnsyncedSubscriberAttributes(appUserID: String, completion: (SubscriberAttributeMap) -> Unit) {
        obtainingDeviceIdentifiersObservable.waitUntilIdle {
            completion(deviceCache.getUnsyncedSubscriberAttributes(appUserID))
        }
    }

    @Synchronized
    fun markAsSynced(
        appUserID: String,
        attributesToMarkAsSynced: Map<String, SubscriberAttribute>,
        attributeErrors: List<SubscriberAttributeError>,
    ) {
        if (attributeErrors.isNotEmpty()) {
            log(LogIntent.RC_ERROR) { AttributionStrings.SUBSCRIBER_ATTRIBUTES_ERROR.format(attributeErrors) }
        }
        if (attributesToMarkAsSynced.isEmpty()) {
            return
        }
        log(LogIntent.INFO) {
            AttributionStrings.MARKING_ATTRIBUTES_SYNCED.format(appUserID) +
                attributesToMarkAsSynced.values.joinToString("\n")
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

    /**
     * Convenience function to set attribution data from AppsFlyer's conversion data.
     */
    @Suppress("CyclomaticComplexMethod")
    public fun setAppsFlyerConversionData(appUserID: String, data: Map<*, *>?) {
        if (data == null) {
            return
        }

        val attributes = mutableMapOf<String, String?>()

        val mediaSource = data.getStringValueForPrimitive("media_source")
        if (mediaSource != null) {
            attributes[SubscriberAttributeKey.CampaignParameters.MediaSource.backendKey] = mediaSource
        } else if (data.getStringValueForPrimitive("af_status")?.equals("Organic", ignoreCase = true) == true) {
            attributes[SubscriberAttributeKey.CampaignParameters.MediaSource.backendKey] = "Organic"
        }

        data.getStringValueForPrimitive("campaign")?.also {
            attributes[SubscriberAttributeKey.CampaignParameters.Campaign.backendKey] = it
        }

        val adGroup = data.getStringValueForPrimitive("adgroup") ?: data.getStringValueForPrimitive("adset")
        adGroup?.also {
            attributes[SubscriberAttributeKey.CampaignParameters.AdGroup.backendKey] = it
        }

        val ad = data.getStringValueForPrimitive("af_ad") ?: data.getStringValueForPrimitive("ad_id")
        ad?.also {
            attributes[SubscriberAttributeKey.CampaignParameters.Ad.backendKey] = it
        }

        val keyword = data.getStringValueForPrimitive("af_keywords") ?: data.getStringValueForPrimitive("keyword")
        keyword?.also {
            attributes[SubscriberAttributeKey.CampaignParameters.Keyword.backendKey] = it
        }

        val creative = data.getStringValueForPrimitive("creative") ?: data.getStringValueForPrimitive("af_creative")
        creative?.also {
            attributes[SubscriberAttributeKey.CampaignParameters.Creative.backendKey] = it
        }

        if (attributes.isNotEmpty()) {
            setAttributes(attributes, appUserID)
        }
    }

    /**
     * Collect GPS ID, ANDROID ID and sets IP to true automatically
     */
    fun collectDeviceIdentifiers(
        appUserID: String,
        applicationContext: Application,
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
        applicationContext: Application,
    ) {
        val setAttributes: (deviceIdentifiers: Map<String, String?>) -> Unit = { deviceIdentifiers ->
            val attributesToSet = mapOf(attributionKey.backendKey to value) + deviceIdentifiers
            setAttributes(attributesToSet, appUserID)
        }
        if (automaticDeviceIdentifierCollectionEnabled) {
            getDeviceIdentifiers(applicationContext) { deviceIdentifiers ->
                setAttributes(deviceIdentifiers)
            }
        } else {
            setAttributes(emptyMap())
        }
    }

    private fun getDeviceIdentifiers(
        applicationContext: Application,
        completion: (deviceIdentifiers: Map<String, String?>) -> Unit,
    ) {
        obtainingDeviceIdentifiersObservable.numberOfProcesses++
        deviceIdentifiersFetcher.getDeviceIdentifiers(applicationContext) { deviceIdentifiers ->
            completion(deviceIdentifiers)
            obtainingDeviceIdentifiersObservable.numberOfProcesses--
        }
    }

    // This was added to resolve: https://revenuecats.atlassian.net/browse/CSDK-433. To summarize it,
    // setting advertising IDs happens on a non-main thread. If that process is delayed for any reason, the attributes
    // won't be stored in the device cache in time for the sync with the backend. This is a mechanism added so we can
    // wait until the device identifiers have been obtained before continuing with other processes.
    private class ObtainDeviceIdentifiersObservable : Observable() {
        var numberOfProcesses: Int = 0
            @Synchronized
            get

            @Synchronized
            set(value) {
                if (field == value) return
                field = value
                setChanged()
                notifyObservers()
            }

        @get:Synchronized
        private val listeners: ArrayList<() -> Unit> = arrayListOf()

        init {
            addObserver { observable, _ ->
                val numberOfProcesses = (observable as ObtainDeviceIdentifiersObservable).numberOfProcesses
                if (numberOfProcesses == 0) {
                    synchronized(this) {
                        listeners.forEach { it() }
                        listeners.clear()
                    }
                }
            }
        }

        @Synchronized
        fun waitUntilIdle(completion: () -> Unit) {
            if (numberOfProcesses == 0) {
                completion()
            } else {
                listeners.add { completion() }
            }
        }
    }
}
