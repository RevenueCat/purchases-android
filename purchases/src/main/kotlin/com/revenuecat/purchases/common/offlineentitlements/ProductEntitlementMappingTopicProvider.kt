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
) {
    suspend fun getProductEntitlementMapping(): ProductEntitlementMapping? =
        manager.blobData(RemoteConfigTopic.ProductEntitlementMapping, ITEM_KEY) { bytes ->
            try {
                ProductEntitlementMapping.fromJson(JSONObject(bytes.decodeToString()))
            } catch (e: JSONException) {
                errorLog(e) { "Failed to parse product entitlement mapping from remote config." }
                null
            }
        }

    private companion object {
        private const val ITEM_KEY = "default"
    }
}
