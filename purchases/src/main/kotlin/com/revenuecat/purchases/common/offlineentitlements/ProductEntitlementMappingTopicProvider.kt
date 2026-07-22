package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import org.json.JSONException
import org.json.JSONObject

/** Decodes the `product_entitlement_mapping.default` remote-config blob. */
@OptIn(InternalRevenueCatAPI::class)
internal class ProductEntitlementMappingTopicProvider(
    private val manager: RemoteConfigManager,
) : EntitlementMappingTopicProvider {
    override suspend fun getProductEntitlementMapping(): ProductEntitlementMappingResult? {
        val blobData = manager.blobDataSnapshot(RemoteConfigTopic.ProductEntitlementMapping, ITEM_KEY) { bytes ->
            try {
                ProductEntitlementMapping.fromJson(JSONObject(bytes.decodeToString()))
            } catch (e: JSONException) {
                errorLog(e) { "Failed to parse product entitlement mapping from remote config." }
                null
            }
        } ?: return null
        return ProductEntitlementMappingResult(blobData.value) { action ->
            manager.useIfCurrent(blobData, action)
        }
    }

    private companion object {
        private const val ITEM_KEY = "default"
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal interface EntitlementMappingTopicProvider {
    suspend fun getProductEntitlementMapping(): ProductEntitlementMappingResult?
}

@OptIn(InternalRevenueCatAPI::class)
internal class ProductEntitlementMappingResult(
    val mapping: ProductEntitlementMapping,
    private val useIfCurrent: ((ProductEntitlementMapping) -> Unit) -> Boolean,
) {
    fun cacheIfCurrent(action: (ProductEntitlementMapping) -> Unit): Boolean = useIfCurrent.invoke(action)
}
