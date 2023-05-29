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
const val ONE_OFFERINGS_INAPP_PRODUCT_RESPONSE = "{'offerings': [" +
    "{'identifier': '$STUB_OFFERING_IDENTIFIER', " +
    "'description': 'This is the base offering', " +
    "'packages': [" +
    "{'identifier': '\$rc_monthly','platform_product_identifier': '$STUB_PRODUCT_IDENTIFIER'}]}]," +
    "'current_offering_id': '$STUB_OFFERING_IDENTIFIER'}"

@SuppressWarnings("EmptyFunctionBlock")
fun stubStoreProduct(
    productId: String,
    defaultOption: SubscriptionOption? = stubSubscriptionOption(
        "monthly_base_plan", productId,
        Period(1, Period.Unit.MONTH, "P1M"),
    ),
    subscriptionOptions: List<SubscriptionOption>? = defaultOption?.let { listOf(defaultOption) } ?: emptyList(),
    price: Price = subscriptionOptions?.first()?.fullPricePhase!!.price,
    presentedOfferingId: String? = null
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
        get() = subscriptionOptions?.firstOrNull { it.isBasePlan }?.pricingPhases?.get(0)?.billingPeriod
    override val subscriptionOptions: SubscriptionOptions?
        get() {
            return subscriptionOptions?.let {
                SubscriptionOptions(it.map { option ->
                    stubSubscriptionOption(
                        id = option.id,
                        productId = productId,
                        duration = option.billingPeriod!!,
                        pricingPhases = option.pricingPhases,
                        presentedOfferingId = presentedOfferingId
                    )
                })
            }
        }
    override val defaultOption: SubscriptionOption?
        get() {
            return defaultOption?.let {
                stubSubscriptionOption(
                    id = it.id,
                    productId = productId,
                    duration = it.billingPeriod!!,
                    pricingPhases = it.pricingPhases,
                    presentedOfferingId = presentedOfferingId
                )
            }
        }
    override val purchasingData: PurchasingData
        get() = StubPurchasingData(
            productId = productId
        )
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingId
    override val sku: String
        get() = productId

    override fun copyWithOfferingId(offeringId: String): StoreProduct {
        val subscriptionOptionsWithOfferingIds = subscriptionOptions?.map {
            stubSubscriptionOption(
                it.id,
                productId,
                period!!,
                it.pricingPhases,
                offeringId
            )
        }

        val defaultOptionWithOfferingId = defaultOption?.let {
            stubSubscriptionOption(
                it.id,
                productId,
                period!!,
                it.pricingPhases,
                offeringId
            )
        }
        return stubStoreProduct(
            productId,
            defaultOptionWithOfferingId,
            subscriptionOptionsWithOfferingIds,
            price,
            offeringId
        )
    }
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubINAPPStoreProduct(
    productId: String,
    presentedOfferingId: String? = null
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
        return object : StoreProduct {
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
            override val presentedOfferingIdentifier: String?
                get() = offeringId
            override val sku: String
                get() = productId

            override fun copyWithOfferingId(offeringId: String): StoreProduct = this
        }
    }
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubSubscriptionOption(
    id: String,
    productId: String,
    duration: Period = Period(1, Period.Unit.MONTH, "P1M"),
    pricingPhases: List<PricingPhase> = listOf(stubPricingPhase(billingPeriod = duration)),
    presentedOfferingId: String? = null
): SubscriptionOption = object : SubscriptionOption {
    override val id: String
        get() = id
    override val pricingPhases: List<PricingPhase>
        get() = pricingPhases
    override val tags: List<String>
        get() = listOf("tag")
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingId
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
        STUB_OFFERING_IDENTIFIER
    )
    val offering = Offering(
        STUB_OFFERING_IDENTIFIER,
        "This is the base offering",
        emptyMap(),
        listOf(packageObject)
    )
    val offerings = Offerings(
        offering,
        mapOf(offering.identifier to offering)
    )
    return Pair(storeProduct, offerings)
}

fun stubOTPOffering(inAppProduct: StoreProduct): Pair<StoreProduct, Offerings> {
    val packageObject = Package(
        "${inAppProduct.id} package",
        PackageType.CUSTOM,
        inAppProduct,
        STUB_OFFERING_IDENTIFIER
    )
    val offering = Offering(
        STUB_OFFERING_IDENTIFIER,
        "This is the base offering",
        emptyMap(),
        listOf(packageObject)
    )
    val offerings = Offerings(
        offering,
        mapOf(offering.identifier to offering)
    )
    return Pair(inAppProduct, offerings)
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
