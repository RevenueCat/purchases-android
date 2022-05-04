package com.revenuecat.purchases.amazon.helpers

import com.amazon.device.iap.internal.model.ProductBuilder
import com.amazon.device.iap.internal.model.ReceiptBuilder
import com.amazon.device.iap.internal.model.UserDataBuilder
import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.ProductType
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserData
import java.util.Date

@SuppressWarnings("LongParameterList")
fun dummyAmazonProduct(
    sku: String = "premium",
    productType: ProductType = ProductType.SUBSCRIPTION,
    description: String = "A product description",
    title: String = "A product title",
    price: String? = "$3.00",
    smallIconUrl: String = "https://icon.url",
    coinsRewardAmount: Int = 100
): Product {
    return ProductBuilder()
        .setSku(sku)
        .setProductType(productType)
        .setDescription(description)
        .setPrice(price)
        .setSmallIconUrl(smallIconUrl)
        .setTitle(title)
        .setCoinsRewardAmount(coinsRewardAmount).build()
}

fun dummyReceipt(
    sku: String = "premium",
    productType: ProductType = ProductType.SUBSCRIPTION,
    purchaseDate: Date = Date(),
    receiptId: String = "receipt_id",
    cancelDate: Date? = Date()
): Receipt {
    return ReceiptBuilder()
        .setReceiptId(receiptId)
        .setCancelDate(cancelDate.takeIf { productType == ProductType.SUBSCRIPTION })
        .setProductType(productType)
        .setPurchaseDate(purchaseDate)
        .setSku(sku)
        .build()
}

fun dummyUserData(
    marketplace: String = "US"
): UserData = UserDataBuilder()
    .setUserId("user_id")
    .setMarketplace(marketplace)
    .build()
