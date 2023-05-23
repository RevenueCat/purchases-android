package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.strings.OfflineEntitlementsStrings

class OfflineEntitlementsManager(
    private val backend: Backend,
    private val offlineCustomerInfoCalculator: OfflineCustomerInfoCalculator,
    private val deviceCache: DeviceCache,
    private val appConfig: AppConfig
) {
    // We cache the offline customer info in memory, so it's not persisted.
    val offlineCustomerInfo: CustomerInfo?
        get() = _offlineCustomerInfo
    @get:Synchronized @set:Synchronized
    private var _offlineCustomerInfo: CustomerInfo? = null

    private val offlineCustomerInfoCallbackCache = mutableMapOf<String, List<OfflineCustomerInfoCallback>>()

    @Synchronized
    fun resetOfflineCustomerInfoCache() {
        if (_offlineCustomerInfo != null) {
            debugLog(OfflineEntitlementsStrings.RESETTING_OFFLINE_CUSTOMER_INFO_CACHE)
            _offlineCustomerInfo = null
        }
    }

    fun shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest(
        isServerError: Boolean,
        appUserId: String
    ) = isServerError &&
        isOfflineEntitlementsEnabled() &&
        deviceCache.getCachedCustomerInfo(appUserId) == null

    fun shouldCalculateOfflineCustomerInfoInPostReceipt(
        isServerError: Boolean
    ) = isServerError && isOfflineEntitlementsEnabled()

    @Suppress("FunctionOnlyReturningConstant")
    fun calculateAndCacheOfflineCustomerInfo(
        appUserId: String,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        synchronized(this@OfflineEntitlementsManager) {
            val alreadyProcessing = offlineCustomerInfoCallbackCache.containsKey(appUserId)
            val callbacks = offlineCustomerInfoCallbackCache[appUserId] ?: emptyList()
            offlineCustomerInfoCallbackCache[appUserId] = callbacks + listOf(onSuccess to onError)
            if (alreadyProcessing) {
                debugLog(OfflineEntitlementsStrings.ALREADY_CALCULATING_OFFLINE_CUSTOMER_INFO.format(appUserId))
                return
            }
        }
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserId,
            onSuccess = { customerInfo ->
                synchronized(this@OfflineEntitlementsManager) {
                    debugLog(OfflineEntitlementsStrings.UPDATING_OFFLINE_CUSTOMER_INFO_CACHE)
                    _offlineCustomerInfo = customerInfo
                    deviceCache.getCachedAppUserID()?.let { deviceCache.clearCustomerInfoCache(it) }
                    val callbacks = offlineCustomerInfoCallbackCache.remove(appUserId)
                    callbacks?.forEach { (onSuccess, _) ->
                        onSuccess(customerInfo)
                    }
                }
            },
            onError = {
                synchronized(this@OfflineEntitlementsManager) {
                    val callbacks = offlineCustomerInfoCallbackCache.remove(appUserId)
                    callbacks?.forEach { (_, onError) ->
                        onError(it)
                    }
                }
            }
        )
    }

    fun updateProductEntitlementMappingCacheIfStale(completion: ((PurchasesError?) -> Unit)? = null) {
        if (isOfflineEntitlementsEnabled() && deviceCache.isProductEntitlementMappingCacheStale()) {
            debugLog(OfflineEntitlementsStrings.UPDATING_PRODUCT_ENTITLEMENT_MAPPING)
            backend.getProductEntitlementMapping(
                onSuccessHandler = { productEntitlementMapping ->
                    deviceCache.cacheProductEntitlementMapping(productEntitlementMapping)
                    debugLog(OfflineEntitlementsStrings.SUCCESSFULLY_UPDATED_PRODUCT_ENTITLEMENTS)
                    completion?.invoke(null)
                },
                onErrorHandler = { e ->
                    errorLog(OfflineEntitlementsStrings.ERROR_UPDATING_PRODUCT_ENTITLEMENTS.format(e))
                    completion?.invoke(e)
                }
            )
        } else {
            completion?.invoke(null)
        }
    }

    // We disable offline entitlements in observer mode (finishTransactions = true) since it doesn't
    // provide any value and simplifies operations in that mode.
    private fun isOfflineEntitlementsEnabled() = appConfig.finishTransactions
}

private typealias OfflineCustomerInfoCallback = Pair<(CustomerInfo) -> Unit, (PurchasesError) -> Unit>
