package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.AmazonPlatformProductId
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PlatformProductId
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import org.json.JSONObject

data class AmazonStoreProduct(
    override val id: String,
    override val type: ProductType,
    override val title: String,
    override val description: String,
    override val period: Period?,
    override val price: Price,
    override val subscriptionOptions: SubscriptionOptions?,
    override val defaultOption: SubscriptionOption?,
    val iconUrl: String,
    val freeTrialPeriod: Period?,
    val originalProductJSON: JSONObject
) : StoreProduct {

    override val purchasingData: AmazonPurchasingData
        get() = AmazonPurchasingData.Product(this)

    override val platformProductId: PlatformProductId
        get() = AmazonPlatformProductId(id)

    @Deprecated(
        "Replaced with id",
        ReplaceWith("id")
    )
    override val sku: String
        get() = id
}

val StoreProduct.amazonProduct: AmazonStoreProduct?
    get() = this as? AmazonStoreProduct
