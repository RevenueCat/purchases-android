package com.revenuecat.purchases.identity

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.SubscriberAttributesCache
import com.revenuecat.purchases.common.debugLog
import java.util.Locale
import java.util.UUID

class IdentityManager(
    private val deviceCache: DeviceCache,
    private val subscriberAttributesCache: SubscriberAttributesCache,
    private val backend: Backend
) {

    val currentAppUserID: String
        get() = deviceCache.getCachedAppUserID() ?: ""

    @Synchronized
    fun configure(appUserID: String?) {
        val appUserIDToUse = appUserID
            ?: deviceCache.getCachedAppUserID()
            ?: deviceCache.getLegacyCachedAppUserID()
            ?: generateRandomID()
        debugLog("Identifying App User ID: $appUserIDToUse")
        deviceCache.cacheAppUserID(appUserIDToUse)
        subscriberAttributesCache.cleanUpSubscriberAttributeCache(appUserIDToUse)
    }

    fun identify(
        appUserID: String,
        onSuccess: () -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        if (currentUserIsAnonymous()) {
            debugLog("Identifying from an anonymous ID: $appUserID. An alias will be created.")
            createAlias(appUserID, onSuccess, onError)
        } else {
            synchronized(this@IdentityManager) {
                debugLog("Changing App User ID: $currentAppUserID -> $appUserID")
                deviceCache.clearCachesForAppUserID()
                subscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(currentAppUserID)
                deviceCache.cacheAppUserID(appUserID)
            }
            onSuccess()
        }
    }

    fun createAlias(
        newAppUserID: String,
        onSuccess: () -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        debugLog("Creating an alias to $currentAppUserID from $newAppUserID")
        backend.createAlias(
            currentAppUserID,
            newAppUserID,
            {
                synchronized(this@IdentityManager) {
                    debugLog("Alias created")
                    deviceCache.clearCachesForAppUserID()
                    subscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(currentAppUserID)
                    deviceCache.cacheAppUserID(newAppUserID)
                }
                onSuccess()
            },
            onError
        )
    }

    @Synchronized
    fun reset() {
        deviceCache.clearCachesForAppUserID()
        subscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(currentAppUserID)
        deviceCache.cacheAppUserID(generateRandomID())
    }

    @Synchronized
    fun currentUserIsAnonymous(): Boolean {
        val currentAppUserIDLooksAnonymous =
            "^\\\$RCAnonymousID:([a-f0-9]{32})$".toRegex()
                .matches(deviceCache.getCachedAppUserID() ?: "")
        val isLegacyAnonymousAppUserID =
            deviceCache.getCachedAppUserID() == deviceCache.getLegacyCachedAppUserID()
        return currentAppUserIDLooksAnonymous || isLegacyAnonymousAppUserID
    }

    private fun generateRandomID(): String {
        return "\$RCAnonymousID:" + UUID.randomUUID().toString().toLowerCase(Locale.ROOT).replace("-", "")
            .also {
                debugLog("Generated New App User ID - $it")
            }
    }
}
