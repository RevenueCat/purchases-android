package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.strings.OfflineEntitlementsStrings

class OfflineEntitlementsManager(
    private val backend: Backend,
    private val offlineCustomerInfoCalculator: OfflineCustomerInfoCalculator,
    private val deviceCache: DeviceCache
) {
    // We cache the offline customer info in memory, so it's not persisted.
    val offlineCustomerInfo: CustomerInfo?
        get() = _offlineCustomerInfo
    // TODO make sure we handle concurrency
    private var _offlineCustomerInfo: CustomerInfo? = null

    fun resetOfflineCustomerInfoCache() {
        if (_offlineCustomerInfo != null) {
            // TODO improve logs
            warnLog("Resetting offline customer info cache")
        }
        _offlineCustomerInfo = null
    }

    fun shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest(
        isServerError: Boolean,
        appUserId: String
    ): Boolean {
        return isServerError && deviceCache.getCachedCustomerInfo(appUserId) == null
    }

    fun shouldCalculateOfflineCustomerInfoInPostReceipt(isServerError: Boolean): Boolean {
        return isServerError
    }

    @Suppress("FunctionOnlyReturningConstant")
    fun calculateAndCacheOfflineCustomerInfo(
        appUserId: String,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserId,
            onSuccess = { customerInfo ->
                _offlineCustomerInfo = customerInfo
                onSuccess(customerInfo)
            },
            onError = onError
        )
    }

    fun updateProductEntitlementMappingCacheIfStale() {
        if (deviceCache.isProductEntitlementMappingCacheStale()) {
            debugLog(OfflineEntitlementsStrings.UPDATING_PRODUCT_ENTITLEMENT_MAPPING)
            backend.getProductEntitlementMapping(
                onSuccessHandler = { productEntitlementMapping ->
                    deviceCache.cacheProductEntitlementMapping(productEntitlementMapping)
                    debugLog(OfflineEntitlementsStrings.SUCCESSFULLY_UPDATED_PRODUCT_ENTITLEMENTS)
                },
                onErrorHandler = { e ->
                    errorLog(OfflineEntitlementsStrings.ERROR_UPDATING_PRODUCT_ENTITLEMENTS.format(e))
                }
            )
        }
    }
}
