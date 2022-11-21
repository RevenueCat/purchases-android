package com.revenuecat.purchases.utils

import android.os.Parcel
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.toRecurrenceMode

fun stubStoreProduct(
    productId: String,
    duration: String = "P1M",
    purchaseOptions: List<PurchaseOption> = listOf(stubPurchaseOption("monthly_base_plan"))
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
    override val subscriptionPeriod: String
        get() = duration
    override val purchaseOptions: List<PurchaseOption>
        get() = purchaseOptions
    override val sku: String
        get() = productId

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {}
}

fun stubPurchaseOption(
    id: String,
    pricingPhases: List<PricingPhase> = listOf(stubPricingPhase())
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
    priceAmountMicros = price.times(1_000_000).toLong(),
    recurrenceMode.toRecurrenceMode(),
    billingCycleCount
)
