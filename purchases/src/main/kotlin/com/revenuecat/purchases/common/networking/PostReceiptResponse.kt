package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.utils.toMap
import org.json.JSONObject

internal fun buildPostReceiptResponse(result: HTTPResult) = PostReceiptResponse(
    customerInfo = CustomerInfoFactory.buildCustomerInfo(result),
    productInfoByProductId = result.body.optJSONObject("purchased_products")?.let {
        it.toMap<JSONObject>().mapValues { (_, value) ->
            PostReceiptProductInfo(
                shouldConsume = value.optBoolean("should_consume"),
            )
        }
    },
    body = result.body,
)

internal data class PostReceiptProductInfo(
    val shouldConsume: Boolean,
)

internal data class PostReceiptResponse(
    val customerInfo: CustomerInfo,
    val productInfoByProductId: Map<String, PostReceiptProductInfo>?,
    val body: JSONObject,
)
