package com.revenuecat.purchases.identity

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.infoLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.offerings.OfferingsCache
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.strings.IdentityStrings
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import java.util.Locale
import java.util.UUID

class IdentityManager(
    private val deviceCache: DeviceCache,
    private val subscriberAttributesCache: SubscriberAttributesCache,
    private val subscriberAttributesManager: SubscriberAttributesManager,
    private val offeringsCache: OfferingsCache,
    private val backend: Backend,
    private val offlineEntitlementsManager: OfflineEntitlementsManager,
) {

    val currentAppUserID: String
        get() = deviceCache.getCachedAppUserID() ?: ""

    private val anonymousIdRegex = "^\\\$RCAnonymousID:([a-f0-9]{32})$".toRegex()

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
        deviceCache.cleanupOldAttributionData()
        invalidateCustomerInfoAndETagCacheIfNeeded(appUserIDToUse)
    }

    fun logIn(
        newAppUserID: String,
        onSuccess: (CustomerInfo, Boolean) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        if (newAppUserID.isBlank()) {
            onError(
                PurchasesError(
                    PurchasesErrorCode.InvalidAppUserIdError,
                    IdentityStrings.LOG_IN_ERROR_MISSING_APP_USER_ID,
                ).also { errorLog(it) },
            )
            return
        }

        log(LogIntent.USER, IdentityStrings.LOGGING_IN.format(currentAppUserID, newAppUserID))
        val oldAppUserID = currentAppUserID
        subscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(newAppUserID) {
            backend.logIn(
                oldAppUserID,
                newAppUserID,
                { customerInfo, created ->
                    synchronized(this@IdentityManager) {
                        log(
                            LogIntent.USER,
                            IdentityStrings.LOG_IN_SUCCESSFUL.format(newAppUserID, created),
                        )
                        deviceCache.clearCachesForAppUserID(oldAppUserID)
                        offeringsCache.clearCache()
                        subscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(oldAppUserID)

                        deviceCache.cacheAppUserID(newAppUserID)
                        deviceCache.cacheCustomerInfo(newAppUserID, customerInfo)
                        copySubscriberAttributesToNewUserIfOldIsAnonymous(oldAppUserID, newAppUserID)
                        offlineEntitlementsManager.resetOfflineCustomerInfoCache()
                    }
                    onSuccess(customerInfo, created)
                },
                onError,
            )
        }
    }

    @Synchronized
    private fun reset() {
        deviceCache.clearCachesForAppUserID(currentAppUserID)
        offeringsCache.clearCache()
        subscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(currentAppUserID)
        offlineEntitlementsManager.resetOfflineCustomerInfoCache()
        deviceCache.cacheAppUserID(generateRandomID())
    }

    @Synchronized
    fun logOut(completion: ((PurchasesError?) -> Unit)) {
        if (currentUserIsAnonymous()) {
            log(LogIntent.RC_ERROR, IdentityStrings.LOG_OUT_CALLED_ON_ANONYMOUS_USER)
            completion(PurchasesError(PurchasesErrorCode.LogOutWithAnonymousUserError))
            return
        }
        subscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(currentAppUserID) {
            reset()
            log(LogIntent.USER, IdentityStrings.LOG_OUT_SUCCESSFUL)
            completion(null)
        }
    }

    @Synchronized
    fun currentUserIsAnonymous(): Boolean {
        val currentAppUserIDLooksAnonymous = isUserIDAnonymous(deviceCache.getCachedAppUserID() ?: "")
        val isLegacyAnonymousAppUserID =
            deviceCache.getCachedAppUserID() == deviceCache.getLegacyCachedAppUserID()
        return currentAppUserIDLooksAnonymous || isLegacyAnonymousAppUserID
    }

    private fun copySubscriberAttributesToNewUserIfOldIsAnonymous(oldAppUserId: String, newAppUserId: String) {
        if (isUserIDAnonymous(oldAppUserId)) {
            subscriberAttributesManager.copyUnsyncedSubscriberAttributes(oldAppUserId, newAppUserId)
        }
    }

    private fun invalidateCustomerInfoAndETagCacheIfNeeded(appUserID: String) {
        val cachedCustomerInfo = deviceCache.getCachedCustomerInfo(appUserID)
        if (shouldInvalidateCustomerInfoAndETagCache(cachedCustomerInfo)) {
            infoLog(IdentityStrings.INVALIDATING_CACHED_CUSTOMER_INFO)
            deviceCache.clearCustomerInfoCache(appUserID)
            backend.clearCaches()
        }
    }

    @Suppress("UnusedPrivateMember", "FunctionOnlyReturningConstant")
    private fun shouldInvalidateCustomerInfoAndETagCache(customerInfo: CustomerInfo?): Boolean {
        return customerInfo != null &&
            customerInfo.entitlements.verification == VerificationResult.NOT_REQUESTED &&
            backend.verificationMode != SignatureVerificationMode.Disabled
    }

    private fun isUserIDAnonymous(appUserID: String): Boolean {
        return anonymousIdRegex.matches(appUserID)
    }

    private fun generateRandomID(): String {
        return "\$RCAnonymousID:" + UUID.randomUUID().toString().toLowerCase(Locale.ROOT).replace("-", "")
            .also {
                log(LogIntent.USER, IdentityStrings.SETTING_NEW_ANON_ID)
            }
    }
}
