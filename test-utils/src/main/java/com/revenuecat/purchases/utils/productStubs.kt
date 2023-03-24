package com.revenuecat.purchases.utils

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.MICROS_MULTIPLIER
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.models.toRecurrenceMode
import org.json.JSONObject

@SuppressWarnings("MatchingDeclarationName")
private data class StubPurchasingData(
    override val productId: String,
) : PurchasingData {
    override val productType: ProductType
        get() = ProductType.SUBS
}

const val STUB_OFFERING_IDENTIFIER = "offering_a"
const val STUB_PRODUCT_IDENTIFIER = "monthly_freetrial"
const val ONE_OFFERINGS_RESPONSE = "{'offerings': [" +
    "{'identifier': '$STUB_OFFERING_IDENTIFIER', " +
    "'description': 'This is the base offering', " +
    "'packages': [" +
    "{'identifier': '\$rc_monthly','platform_product_identifier': '$STUB_PRODUCT_IDENTIFIER'," +
    "'platform_product_plan_identifier': 'p1m'}]}]," +
    "'current_offering_id': '$STUB_OFFERING_IDENTIFIER'}"

@SuppressWarnings("EmptyFunctionBlock")
fun stubStoreProduct(
    productId: String,
    defaultOption: SubscriptionOption? = stubSubscriptionOption(
        "monthly_base_plan", productId,
        Period(1, Period.Unit.MONTH, "P1M"),
    ),
    subscriptionOptions: List<SubscriptionOption> = defaultOption?.let { listOf(defaultOption) } ?: emptyList(),
    price: Price = subscriptionOptions.first().fullPricePhase!!.price
): StoreProduct = object : StoreProduct {
    override val id: String
        get() = productId
    override val type: ProductType
        get() = ProductType.SUBS
    override val price: Price
        get() = price
    override val title: String
        get() = ""
    override val description: String
        get() = ""
    override val period: Period?
        get() = subscriptionOptions.firstOrNull { it.isBasePlan }?.pricingPhases?.get(0)?.billingPeriod
    override val subscriptionOptions: SubscriptionOptions
        get() = SubscriptionOptions(subscriptionOptions)
    override val defaultOption: SubscriptionOption?
        get() = defaultOption
    override val purchasingData: PurchasingData
        get() = StubPurchasingData(
            productId = productId
        )
    override val sku: String
        get() = productId
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubINAPPStoreProduct(
    productId: String
): StoreProduct = object : StoreProduct {
    override val id: String
        get() = productId
    override val type: ProductType
        get() = ProductType.INAPP
    override val price: Price
        get() = Price("\$1.00", MICROS_MULTIPLIER * 1L, "USD")
    override val title: String
        get() = ""
    override val description: String
        get() = ""
    override val period: Period?
        get() = null
    override val subscriptionOptions: SubscriptionOptions
        get() = SubscriptionOptions(listOf(defaultOption))
    override val defaultOption: SubscriptionOption
        get() = stubSubscriptionOption(productId, productId)
    override val purchasingData: PurchasingData
        get() = StubPurchasingData(
            productId = productId
        )
    override val sku: String
        get() = productId
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubSubscriptionOption(
    id: String,
    productId: String,
    duration: Period = Period(1, Period.Unit.MONTH, "P1M"),
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
}

fun stubFreeTrialPricingPhase(
    billingPeriod: Period = Period(1, Period.Unit.MONTH, "P1M"),
    priceCurrencyCodeValue: String = "USD",
) = stubPricingPhase(
    billingPeriod = billingPeriod,
    priceCurrencyCodeValue = priceCurrencyCodeValue,
    price = 0.0,
    recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
    billingCycleCount = 1
)

fun stubPricingPhase(
    billingPeriod: Period = Period(1, Period.Unit.MONTH, "P1M"),
    priceCurrencyCodeValue: String = "USD",
    price: Double = 4.99,
    recurrenceMode: Int = ProductDetails.RecurrenceMode.INFINITE_RECURRING,
    billingCycleCount: Int = 0
): PricingPhase = PricingPhase(
    billingPeriod,
    recurrenceMode.toRecurrenceMode(),
    billingCycleCount,
    Price(if (price == 0.0) "Free" else "${'$'}$price", price.times(MICROS_MULTIPLIER).toLong(), priceCurrencyCodeValue)
)

fun stubOfferings(storeProduct: StoreProduct): Pair<StoreProduct, Offerings> {
    val packageObject = Package(
        "\$rc_monthly",
        PackageType.MONTHLY,
        storeProduct,
        STUB_OFFERING_IDENTIFIER,
        null
    )
    val offering = Offering(
        STUB_OFFERING_IDENTIFIER,
        "This is the base offering",
        listOf(packageObject)
    )
    val offerings = Offerings(
        offering,
        mapOf(offering.identifier to offering)
    )
    return Pair(storeProduct, offerings)
}

fun stubOfferings(productId: String): Pair<StoreProduct, Offerings> {
    val storeProduct = stubStoreProduct(productId)
    return stubOfferings(storeProduct)
}

fun getLifetimePackageJSON() =
    JSONObject(
        """
                {
                    'identifier': '${PackageType.LIFETIME.identifier}',
                    'platform_product_identifier': 'com.myproduct.lifetime'
                }
            """.trimIndent()
    )

fun getAmazonPackageJSON(
    packageIdentifier: String = "com.myproduct",
    productIdentifier: String = "com.myproduct.monthly"
) =
    JSONObject(
        """
                {
                    'identifier': '$packageIdentifier',
                    'platform_product_identifier': '$productIdentifier'
                }
            """.trimIndent()
    )
