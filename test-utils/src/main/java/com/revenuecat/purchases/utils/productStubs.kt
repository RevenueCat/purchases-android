package com.revenuecat.purchases.utils

import android.os.Parcel
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.MICROS_MULTIPLIER
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.toRecurrenceMode

@SuppressWarnings("EmptyFunctionBlock")
fun stubStoreProduct(
    productId: String,
    purchaseOptions: List<PurchaseOption> = listOf(stubPurchaseOption("monthly_base_plan", "P1M"))
): StoreProduct = object : StoreProduct {
    override val productId: String
        get() = productId
    override val type: ProductType
        get() = ProductType.SUBS
    override val oneTimeProductPrice: Price?
        get() = null
    override val title: String
        get() = ""
    override val description: String
        get() = ""
    override val subscriptionPeriod: String?
        get() = purchaseOptions.firstOrNull { it.isBasePlan }?.pricingPhases?.get(0)?.billingPeriod
    override val purchaseOptions: List<PurchaseOption>
        get() = purchaseOptions
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
    override val purchaseOptions: List<PurchaseOption>
        get() = listOf(stubPurchaseOption(productId))
    override val sku: String
        get() = productId

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {}
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubPurchaseOption(
    id: String,
    duration: String = "P1M",
    pricingPhases: List<PricingPhase> = listOf(stubPricingPhase(billingPeriod = duration))
): PurchaseOption = object : PurchaseOption {
    override val id: String
        get() = id
    override val pricingPhases: List<PricingPhase>
        get() = pricingPhases
    override val tags: List<String>
        get() = listOf("tag")

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel?, flags: Int) {}
}

fun stubPricingPhase(
    billingPeriod: String = "P1M",
    priceCurrencyCodeValue: String = "USD",
    price: Double = 4.99,
    recurrenceMode: Int = ProductDetails.RecurrenceMode.INFINITE_RECURRING,
    billingCycleCount: Int = 0
): PricingPhase = PricingPhase(
    billingPeriod,
    priceCurrencyCodeValue,
    formattedPrice = "${'$'}$price",
    priceAmountMicros = price.times(MICROS_MULTIPLIER).toLong(),
    recurrenceMode.toRecurrenceMode(),
    billingCycleCount
)
