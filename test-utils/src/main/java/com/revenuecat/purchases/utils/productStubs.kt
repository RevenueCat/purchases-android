package com.revenuecat.purchases.utils

import android.os.Parcel
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.MICROS_MULTIPLIER
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.toRecurrenceMode

@SuppressWarnings("MatchingDeclarationName")
private data class StubPurchasingData(
    override val productId: String,
) : PurchasingData {
    override val productType: ProductType
        get() = ProductType.SUBS
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubStoreProduct(
    productId: String,
    defaultOption: SubscriptionOption? = stubPurchaseOption("monthly_base_plan", productId, "P1M",),
    subscriptionOptions: List<SubscriptionOption> = defaultOption?.let { listOf(defaultOption) } ?: emptyList(),
    oneTimeProductPrice: Price? = null
): StoreProduct = object : StoreProduct {
    override val productId: String
        get() = productId
    override val type: ProductType
        get() = ProductType.SUBS
    override val oneTimeProductPrice: Price?
        get() = oneTimeProductPrice
    override val title: String
        get() = ""
    override val description: String
        get() = ""
    override val subscriptionPeriod: String?
        get() = subscriptionOptions.firstOrNull { it.isBasePlan }?.pricingPhases?.get(0)?.billingPeriod
    override val subscriptionOptions: List<SubscriptionOption>
        get() = subscriptionOptions
    override val defaultOption: SubscriptionOption?
        get() = defaultOption
    override val purchasingData: PurchasingData
        get() = StubPurchasingData(
            productId = productId
        )
    override val sku: String
        get() = productId

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {}
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubINAPPStoreProduct(
    productId: String
): StoreProduct = object : StoreProduct {
    override val productId: String
        get() = productId
    override val type: ProductType
        get() = ProductType.INAPP
    override val oneTimeProductPrice: Price?
        get() = Price("\$1.00", MICROS_MULTIPLIER * 1L, "USD")
    override val title: String
        get() = ""
    override val description: String
        get() = ""
    override val subscriptionPeriod: String?
        get() = null
    override val subscriptionOptions: List<SubscriptionOption>
        get() = listOf(defaultOption)
    override val defaultOption: SubscriptionOption
        get() = stubPurchaseOption(productId, productId)
    override val purchasingData: PurchasingData
        get() = StubPurchasingData(
            productId = productId
        )
    override val sku: String
        get() = productId

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {}
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubPurchaseOption(
    id: String,
    productId: String,
    duration: String = "P1M",
    pricingPhases: List<PricingPhase> = listOf(stubPricingPhase(billingPeriod = duration))
): SubscriptionOption = object : SubscriptionOption {
    override val id: String
        get() = id
    override val pricingPhases: List<PricingPhase>
        get() = pricingPhases
    override val tags: List<String>
        get() = listOf("tag")
    override val purchasingData: PurchasingData
        get() = StubPurchasingData(
            productId = productId
        )

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel?, flags: Int) {}
}

fun stubFreeTrialPricingPhase(
    billingPeriod: String = "P1M",
    priceCurrencyCodeValue: String = "USD",
) = stubPricingPhase(
    billingPeriod = billingPeriod,
    priceCurrencyCodeValue = priceCurrencyCodeValue,
    price = 0.0,
    recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
    billingCycleCount = 1
)

fun stubPricingPhase(
    billingPeriod: String = "P1M",
    priceCurrencyCodeValue: String = "USD",
    price: Double = 4.99,
    recurrenceMode: Int = ProductDetails.RecurrenceMode.INFINITE_RECURRING,
    billingCycleCount: Int = 0
): PricingPhase = PricingPhase(
    billingPeriod,
    priceCurrencyCodeValue,
    formattedPrice = if (price == 0.0) "Free" else "${'$'}$price",
    priceAmountMicros = price.times(MICROS_MULTIPLIER).toLong(),
    recurrenceMode.toRecurrenceMode(),
    billingCycleCount
)
