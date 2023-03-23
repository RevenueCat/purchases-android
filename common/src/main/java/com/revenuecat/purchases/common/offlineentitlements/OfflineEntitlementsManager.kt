package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.strings.OfflineEntitlementsStrings

class OfflineEntitlementsManager(
    private val backend: Backend,
    private val deviceCache: DeviceCache
) {

    fun updateProductEntitlementMappingsCacheIfStale() {
        if (deviceCache.isProductEntitlementMappingsCacheStale()) {
            debugLog(OfflineEntitlementsStrings.UPDATING_PRODUCT_ENTITLEMENT_MAPPINGS)
            backend.getProductEntitlementMappings(
                onSuccessHandler = { productEntitlementMappings ->
                    deviceCache.cacheProductEntitlementMappings(productEntitlementMappings)
                    debugLog(OfflineEntitlementsStrings.SUCCESSFULLY_UPDATED_PRODUCT_ENTITLEMENTS)
                },
                onErrorHandler = { e ->
                    errorLog(OfflineEntitlementsStrings.ERROR_UPDATING_PRODUCT_ENTITLEMENTS.format(e))
                }
            )
        }
    }
}
