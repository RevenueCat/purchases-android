package com.revenuecat.purchases

import java.util.Locale
import java.util.UUID

internal class IdentityManager(
    private val deviceCache: DeviceCache,
    private val backend: Backend
) {

    val currentAppUserID: String
        get() = deviceCache.getCachedAppUserID()!!

    @Synchronized
    fun configure(appUserID: String?) {
        val appUserIDToUse = appUserID
            ?: deviceCache.getCachedAppUserID()
            ?: deviceCache.getLegacyCachedAppUserID()
            ?: generateRandomID()
        debugLog("Identifying App User ID: $appUserIDToUse")
        deviceCache.cacheAppUserID(appUserIDToUse)
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
                deviceCache.cacheAppUserID(appUserID)
            }
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
                    deviceCache.cacheAppUserID(newAppUserID)
                    onSuccess()
                }
            },
            onError
        )
    }

    @Synchronized
    fun reset() {
        deviceCache.clearCachesForAppUserID()
        deviceCache.cacheAppUserID(generateRandomID())
    }

    @Synchronized
    fun currentUserIsAnonymous(): Boolean {
        val currentAppUserIDLooksAnonymous =
            "^\\\$RCAnonymousID:([a-f0-9]{32})$".toRegex().matches(deviceCache.getCachedAppUserID()?:"")
        val isLegacyAnonymousAppUserID =
            deviceCache.getCachedAppUserID() == deviceCache.getLegacyCachedAppUserID()
        return currentAppUserIDLooksAnonymous || isLegacyAnonymousAppUserID
    }

    private fun generateRandomID(): String {
        return "\$RCAnonymousID:"+ UUID.randomUUID().toString().toLowerCase(Locale.ROOT).replace("-", "")
            .also {
                debugLog("Generated New App User ID - $it")
            }
    }
}
