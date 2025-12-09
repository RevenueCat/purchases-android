package com.revenuecat.purchases.galaxy

import com.samsung.android.sdk.iap.lib.vo.ProductVo

/**
 * Contains helper utilities that are useful when testing the Galaxy Store.
 */
open class GalaxyStoreTest {
    fun createProductVo(
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
                "mItemId": "product_id",
                "mItemName": "Test Product",
                "mItemPrice": $itemPrice,
                "mItemPriceString": "$itemPriceString",
                "mCurrencyUnit": "$currencyUnit",
                "mCurrencyCode": "$currencyCode",
                "mItemDesc": "Test product description",
                "mType": "$type",
                "mSubscriptionDurationMultiplier": "$subscriptionDurationMultiplier",
                "mSubscriptionDurationUnit": "$subscriptionDurationUnit"
            }
        """.trimIndent()

        return ProductVo(json)
    }
}
