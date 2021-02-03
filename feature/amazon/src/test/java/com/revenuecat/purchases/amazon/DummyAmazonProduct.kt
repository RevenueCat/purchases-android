package com.revenuecat.purchases.amazon

import com.amazon.device.iap.internal.model.ProductBuilder
import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.ProductType

@SuppressWarnings("LongParameterList")
fun dummyAmazonProduct(
    sku: String = "subscription.monthly",
    productType: ProductType = ProductType.SUBSCRIPTION,
    description: String = "A product description",
    title: String = "A product title",
    price: String = "$3.00",
    smallIconUrl: String = "https://icon.url",
    coinsRewardAmount: Int = 100
): Product {
    val builder = ProductBuilder()
        .setSku(sku)
        .setProductType(productType)
        .setDescription(description)
        .setPrice(price)
        .setSmallIconUrl(smallIconUrl)
        .setTitle(title)
        .setCoinsRewardAmount(coinsRewardAmount)

    return Product(builder)
}
