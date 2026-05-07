package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.caching.DeviceCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@OptIn(InternalRevenueCatAPI::class)
internal fun interface ProductEntitlementMappingSource {
    fun getProductEntitlementMapping(completion: (ProductEntitlementMapping?) -> Unit)
}

@OptIn(InternalRevenueCatAPI::class)
internal class DeviceCacheProductEntitlementMappingSource(
    private val deviceCache: DeviceCache,
) : ProductEntitlementMappingSource {
    override fun getProductEntitlementMapping(completion: (ProductEntitlementMapping?) -> Unit) {
        completion(deviceCache.getProductEntitlementMapping())
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal class RemoteConfigProductEntitlementMappingSource(
    private val reader: ProductEntitlementMappingTopicReader,
    private val deviceCache: DeviceCache,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProductEntitlementMappingSource {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    override fun getProductEntitlementMapping(completion: (ProductEntitlementMapping?) -> Unit) {
        scope.launch {
            val topicMapping = reader.read()
            completion(topicMapping ?: deviceCache.getProductEntitlementMapping())
        }
    }
}
