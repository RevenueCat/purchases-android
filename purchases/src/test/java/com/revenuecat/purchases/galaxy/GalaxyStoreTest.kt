package com.revenuecat.purchases.galaxy

import com.samsung.android.sdk.iap.lib.vo.ProductVo
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo
import org.json.JSONObject

/**
 * Contains helper utilities that are useful when testing the Galaxy Store.
 */
open class GalaxyStoreTest {
    fun createProductVo(
        itemId: String = "product_id",
        itemName: String = "Test Product",
        itemDescription: String = "Test product description",
        itemPrice: Double = 0.99,
        currencyUnit: String = "$",
        currencyCode: String = "USD",
        type: String = "item",
        itemPriceString: String = "$currencyUnit$itemPrice",
        subscriptionDurationMultiplier: String = "",
        subscriptionDurationUnit: String = "",
    ): ProductVo {
        val json = """
            {
                "mItemId": "$itemId",
                "mItemName": "$itemName",
                "mItemPrice": $itemPrice,
                "mItemPriceString": "$itemPriceString",
                "mCurrencyUnit": "$currencyUnit",
                "mCurrencyCode": "$currencyCode",
                "mItemDesc": "$itemDescription",
                "mType": "$type",
                "mSubscriptionDurationMultiplier": "$subscriptionDurationMultiplier",
                "mSubscriptionDurationUnit": "$subscriptionDurationUnit"
            }
        """.trimIndent()

        return ProductVo(json)
    }

    fun createPurchaseVo(
        paymentId: String,
        purchaseId: String,
        orderId: String,
    ): PurchaseVo {
        val productVo = createProductVo()
        val purchaseJson = JSONObject(productVo.jsonString).apply {
            put("mPaymentId", paymentId)
            put("mPurchaseId", purchaseId)
            put("mOrderId", orderId)
        }
        return PurchaseVo(purchaseJson.toString())
    }
}
