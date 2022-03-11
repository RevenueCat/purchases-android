package com.revenuecat.purchases.identity

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.IdentityStrings
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
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
        if (appUserID?.isBlank() == true) {
            log(LogIntent.WARNING, IdentityStrings.EMPTY_APP_USER_ID_WILL_BECOME_ANONYMOUS)
        }

        val appUserIDToUse = appUserID
            ?.takeUnless { it.isBlank() }
            ?: deviceCache.getCachedAppUserID()
            ?: deviceCache.getLegacyCachedAppUserID()
            ?: generateRandomID()
        log(LogIntent.USER, IdentityStrings.IDENTIFYING_APP_USER_ID.format(appUserIDToUse))
        deviceCache.cacheAppUserID(appUserIDToUse)
        subscriberAttributesCache.cleanUpSubscriberAttributeCache(appUserIDToUse)
        deviceCache.cleanupOldAttributionData(currentAppUserID)
    }

    fun logIn(
        newAppUserID: String,
        onSuccess: (CustomerInfo, Boolean) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        if (newAppUserID.isBlank()) {
            onError(PurchasesError(
                PurchasesErrorCode.InvalidAppUserIdError,
                IdentityStrings.LOG_IN_ERROR_MISSING_APP_USER_ID
            ).also { errorLog(it) })
            return
        }

        log(LogIntent.USER, IdentityStrings.LOGGING_IN.format(currentAppUserID, newAppUserID))
        val oldAppUserID = currentAppUserID
        backend.logIn(
            oldAppUserID,
            newAppUserID,
            { customerInfo, created ->
                synchronized(this@IdentityManager) {
                    log(
                        LogIntent.USER,
                        IdentityStrings.LOG_IN_SUCCESSFUL.format(newAppUserID, created)
                    )
                    deviceCache.clearCachesForAppUserID(oldAppUserID)
                    subscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(oldAppUserID)

                    deviceCache.cacheAppUserID(newAppUserID)
                    deviceCache.cacheCustomerInfo(newAppUserID, customerInfo)
                }
                onSuccess(customerInfo, created)
            },
            onError
        )
    }

    @Synchronized
    private fun reset() {
        deviceCache.clearCachesForAppUserID(currentAppUserID)
        subscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(currentAppUserID)
        deviceCache.cacheAppUserID(generateRandomID())
    }

    @Synchronized
    fun logOut(): PurchasesError? {
        if (currentUserIsAnonymous()) {
            log(LogIntent.RC_ERROR, IdentityStrings.LOG_OUT_CALLED_ON_ANONYMOUS_USER)
            return PurchasesError(PurchasesErrorCode.LogOutWithAnonymousUserError)
        }
        reset()
        log(LogIntent.USER, IdentityStrings.LOG_OUT_SUCCESSFUL)
        return null
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
