package com.revenuecat.purchases.amazon.helpers

import com.amazon.device.iap.internal.model.ProductBuilder
import com.amazon.device.iap.internal.model.ReceiptBuilder
import com.amazon.device.iap.internal.model.UserDataBuilder
import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.ProductType
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserData
import com.revenuecat.purchases.common.MICROS_MULTIPLIER
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import java.util.Date
import java.util.Locale

@SuppressWarnings("LongParameterList")
fun dummyAmazonProduct(
    sku: String = "premium",
    productType: ProductType = ProductType.SUBSCRIPTION,
    description: String = "A product description",
    subscriptionPeriod: String? = null,
    title: String = "A product title",
    price: String? = "$3.00",
    smallIconUrl: String = "https://icon.url",
    freeTrialPeriod: String? = null,
    coinsRewardAmount: Int = 100
): Product {
    return ProductBuilder()
        .setSku(sku)
        .setProductType(productType)
        .setDescription(description)
        .setFreeTrialPeriod(freeTrialPeriod)
        .setPrice(price)
        .setSmallIconUrl(smallIconUrl)
        .setSubscriptionPeriod(subscriptionPeriod)
        .setTitle(title)
        .setCoinsRewardAmount(coinsRewardAmount).build()
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubStoreProductForAmazon(
    productId: String,
    type: com.revenuecat.purchases.ProductType = com.revenuecat.purchases.ProductType.SUBS,
    price: Price = Price("\$1.00", MICROS_MULTIPLIER * 1L, "USD"),
    period: Period = Period(1, Period.Unit.MONTH, "P1M"),
    presentedOfferingId: String? = null
): StoreProduct = object : StoreProduct {
    override val id: String
        get() = productId
    override val type: com.revenuecat.purchases.ProductType
        get() = type
    override val price: Price
        get() = price
    override val name: String
        get() = "An awesome name"
    override val title: String
        get() = "An awesome name (App name)"
    override val description: String
        get() = "An awesome description"
    override val period: Period?
        get() = period
    override val subscriptionOptions: SubscriptionOptions?
        get() = null
    override val defaultOption: SubscriptionOption?
        get() = null
    override val purchasingData: PurchasingData
        get() = StubPurchasingData(
            productId = productId
        )
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingId
    override val sku: String
        get() = productId

    override fun copyWithOfferingId(offeringId: String): StoreProduct {
        return stubStoreProductForAmazon(
            productId,
            type,
            price,
            period,
            offeringId
        )
    }

    override fun formattedPricePerMonth(locale: Locale): String? {
        error("not implemented")
    }
}

@SuppressWarnings("MatchingDeclarationName")
private data class StubPurchasingData(
    override val productId: String,
) : PurchasingData {
    override val productType: com.revenuecat.purchases.ProductType
        get() = com.revenuecat.purchases.ProductType.SUBS
}

fun dummyReceipt(
    sku: String = "premium",
    productType: ProductType = ProductType.SUBSCRIPTION,
    purchaseDate: Date = Date(),
    receiptId: String = "receipt_id",
    cancelDate: Date? = null
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
    marketplace: String = "US",
    storeUserId: String = "user_id"
): UserData = UserDataBuilder()
    .setUserId(storeUserId)
    .setMarketplace(marketplace)
    .build()
