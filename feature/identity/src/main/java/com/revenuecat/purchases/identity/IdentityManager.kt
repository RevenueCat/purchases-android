package com.revenuecat.purchases.identity

import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.IdentityStrings
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import java.util.*

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
        log(LogIntent.USER, IdentityStrings.IDENTIFYING_APP_USER_ID.format(appUserIDToUse))
        deviceCache.cacheAppUserID(appUserIDToUse)
        subscriberAttributesCache.cleanUpSubscriberAttributeCache(appUserIDToUse)
    }

    fun identify(
        appUserID: String,
        onSuccess: () -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        if (currentUserIsAnonymous()) {
            log(LogIntent.USER, IdentityStrings.IDENTIFYING_ANON_ID.format(appUserID))
            createAlias(appUserID, onSuccess, onError)
        } else {
            synchronized(this@IdentityManager) {
                log(LogIntent.USER, IdentityStrings.CHANGING_APP_USER_ID.format(currentAppUserID, appUserID))
                deviceCache.clearCachesForAppUserID(currentAppUserID)
                subscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(currentAppUserID)
                deviceCache.cacheAppUserID(appUserID)
            }
            onSuccess()
        }
    }

    fun logIn(
            appUserID: String,
            onSuccess: (PurchaserInfo, Boolean) -> Unit,
            onError: (PurchasesError) -> Unit
    ) {
        onError(PurchasesError(PurchasesErrorCode.UnknownError, "the method hasn't been implemented yet"))
    }

    fun createAlias(
        newAppUserID: String,
        onSuccess: () -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        log(LogIntent.USER, IdentityStrings.CREATING_ALIAS.format(currentAppUserID, newAppUserID))
        backend.createAlias(
            currentAppUserID,
            newAppUserID,
            {
                synchronized(this@IdentityManager) {
                    log(LogIntent.USER, IdentityStrings.CREATING_ALIAS_SUCCESS)
                    deviceCache.clearCachesForAppUserID(currentAppUserID)
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
        deviceCache.clearCachesForAppUserID(currentAppUserID)
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
                log(LogIntent.USER, IdentityStrings.SETTING_NEW_ANON_ID)
            }
    }
}
