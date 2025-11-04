package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.common.HTTPResponseOriginalSource
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.originalDataSource
import com.revenuecat.purchases.utils.optNullableString
import org.json.JSONArray
import org.json.JSONObject

internal data class ProductEntitlementMapping(
    val mappings: Map<String, Mapping>,
    internal val originalSource: HTTPResponseOriginalSource = HTTPResponseOriginalSource.MAIN,
    val loadedFromCache: Boolean = false,
) {
    companion object {
        private const val PRODUCT_ENTITLEMENT_MAPPING_KEY = "product_entitlement_mapping"
        private const val PRODUCT_ID_KEY = "product_identifier"
        private const val BASE_PLAN_ID_KEY = "base_plan_id"
        private const val ENTITLEMENTS_KEY = "entitlements"
        private const val ORIGINAL_SOURCE_KEY = "rc_original_source"

        fun fromJson(
            json: JSONObject,
            originalSource: HTTPResponseOriginalSource = HTTPResponseOriginalSource.MAIN,
            loadedFromCache: Boolean = false,
        ): ProductEntitlementMapping {
            val productsObject = json.getJSONObject(PRODUCT_ENTITLEMENT_MAPPING_KEY)
            val mappings = mutableMapOf<String, Mapping>()
            for (mappingIdentifier in productsObject.keys()) {
                val productObject = productsObject.getJSONObject(mappingIdentifier)
                val productIdentifier = productObject.getString(PRODUCT_ID_KEY)
                val basePlanId = productObject.optNullableString(BASE_PLAN_ID_KEY)
                val entitlementsArray = productObject.getJSONArray(ENTITLEMENTS_KEY)
                val entitlements = mutableListOf<String>()
                for (entitlementIndex in 0 until entitlementsArray.length()) {
                    entitlements.add(entitlementsArray.getString(entitlementIndex))
                }
                mappings[mappingIdentifier] = Mapping(productIdentifier, basePlanId, entitlements)
            }
            return ProductEntitlementMapping(mappings, originalSource, loadedFromCache)
        }

        fun fromNetwork(json: JSONObject, httpResult: HTTPResult): ProductEntitlementMapping {
            val originalSource = httpResult.originalDataSource
            return fromJson(json, originalSource, loadedFromCache = false)
        }
    }

    data class Mapping(
        val productIdentifier: String,
        val basePlanId: String?,
        val entitlements: List<String>,
    )

    fun toJson() = JSONObject().apply {
        val mappingsObjects = mappings.mapValues { (_, value) ->
            JSONObject().apply {
                put(PRODUCT_ID_KEY, value.productIdentifier)
                value.basePlanId?.let { put(BASE_PLAN_ID_KEY, it) }
                put(ENTITLEMENTS_KEY, JSONArray(value.entitlements))
            }
        }
        put(PRODUCT_ENTITLEMENT_MAPPING_KEY, JSONObject(mappingsObjects))
        put(ORIGINAL_SOURCE_KEY, originalSource.name)
    }
}
