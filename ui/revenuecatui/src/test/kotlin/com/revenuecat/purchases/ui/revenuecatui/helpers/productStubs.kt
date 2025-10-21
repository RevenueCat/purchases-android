package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.SharedConstants.MICRO_MULTIPLIER
import com.revenuecat.purchases.models.GoogleInstallmentsInfo
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.InstallmentsInfo
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.models.toRecurrenceMode
import io.mockk.mockk

@SuppressWarnings("MatchingDeclarationName")
private data class StubPurchasingData(
    override val productId: String,
    override val productType: ProductType = ProductType.SUBS,
) : PurchasingData

@SuppressWarnings("EmptyFunctionBlock")
fun stubStoreProduct(
    productId: String,
    defaultOption: SubscriptionOption? = stubSubscriptionOption(
        "monthly_base_plan",
        productId,
        Period(1, Period.Unit.MONTH, "P1M"),
    ),
    subscriptionOptions: List<SubscriptionOption>? = defaultOption?.let { listOf(defaultOption) } ?: emptyList(),
    price: Price = subscriptionOptions?.first()?.fullPricePhase!!.price,
    presentedOfferingContext: PresentedOfferingContext? = null,
): StoreProduct = object : StoreProduct {
    override val id: String
        get() = productId
    override val type: ProductType
        get() = ProductType.SUBS
    override val price: Price
        get() = price
    override val name: String
        get() = ""
    override val title: String
        get() = ""
    override val description: String
        get() = ""
    override val period: Period?
        get() = subscriptionOptions?.firstOrNull { it.isBasePlan }?.pricingPhases?.get(0)?.billingPeriod
    override val subscriptionOptions: SubscriptionOptions?
        get() {
            return subscriptionOptions?.let {
                SubscriptionOptions(
                    it.map { option ->
                        stubSubscriptionOption(
                            id = option.id,
                            productId = productId,
                            duration = option.billingPeriod!!,
                            pricingPhases = option.pricingPhases,
                            presentedOfferingContext = presentedOfferingContext,
                        )
                    },
                )
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
                    presentedOfferingContext = presentedOfferingContext,
                )
            }
        }
    override val purchasingData: PurchasingData
        get() = StubPurchasingData(
            productId = productId,
        )
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext?.offeringIdentifier
    override val presentedOfferingContext: PresentedOfferingContext?
        get() = presentedOfferingContext
    override val sku: String
        get() = productId

    override fun copyWithPresentedOfferingContext(presentedOfferingContext: PresentedOfferingContext?): StoreProduct {
        val subscriptionOptionsWithOfferingIds = subscriptionOptions?.map {
            stubSubscriptionOption(
                it.id,
                productId,
                period!!,
                it.pricingPhases,
                presentedOfferingContext,
            )
        }

        val defaultOptionWithOfferingId = defaultOption?.let {
            stubSubscriptionOption(
                it.id,
                productId,
                period!!,
                it.pricingPhases,
                presentedOfferingContext,
            )
        }
        return stubStoreProduct(
            productId,
            defaultOptionWithOfferingId,
            subscriptionOptionsWithOfferingIds,
            price,
            presentedOfferingContext,
        )
    }

    override fun copyWithOfferingId(offeringId: String): StoreProduct {
        return copyWithPresentedOfferingContext(PresentedOfferingContext(offeringId))
    }
}

@Suppress("LongParameterList")
fun createGoogleStoreProduct(
    productId: String,
    basePlanId: String,
    type: ProductType = ProductType.SUBS,
    productDetails: ProductDetails = mockk(),
    subscriptionOptions: List<SubscriptionOption>? = listOf(
        stubGoogleSubscriptionOption(
            productId = productId,
            basePlanId = basePlanId,
            productDetails = productDetails
        )
    ),
    price: Price = subscriptionOptions?.first()?.fullPricePhase!!.price,
    name: String = "Monthly Product Intro Pricing One Week",
    title: String = "Monthly Product Intro Pricing One Week (RevenueCat SDK Tester)",
    description: String = "Monthly Product Intro Pricing One Week",
    period: Period? = subscriptionOptions?.first()?.fullPricePhase!!.billingPeriod,
    defaultOptionIndex: Int = 0,
): StoreProduct {
    val subscriptionOptions = subscriptionOptions?.let { SubscriptionOptions(it) }
    return GoogleStoreProduct(
        productId = productId,
        basePlanId = basePlanId,
        type = type,
        price = price,
        name = name,
        title = title,
        description = description,
        period = period,
        subscriptionOptions = subscriptionOptions,
        defaultOption = subscriptionOptions?.let { it[defaultOptionIndex] },
        productDetails = productDetails,
        presentedOfferingContext = null,
    )
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubINAPPStoreProduct(
    productId: String,
    presentedOfferingContext: PresentedOfferingContext? = null,
): StoreProduct = object : StoreProduct {
    override val id: String
        get() = productId
    override val type: ProductType
        get() = ProductType.INAPP
    override val price: Price
        get() = Price("\$1.00", MICRO_MULTIPLIER.toLong(), "USD")
    override val name: String
        get() = ""
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
            productId = productId, productType = ProductType.INAPP
        )
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext?.offeringIdentifier
    override val presentedOfferingContext: PresentedOfferingContext?
        get() = presentedOfferingContext
    override val sku: String
        get() = productId

    override fun copyWithPresentedOfferingContext(presentedOfferingContext: PresentedOfferingContext?): StoreProduct {
        return object : StoreProduct {
            override val id: String
                get() = productId
            override val type: ProductType
                get() = ProductType.INAPP
            override val price: Price
                get() = Price("\$1.00", MICRO_MULTIPLIER.toLong(), "USD")
            override val name: String
                get() = ""
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
                    productId = productId,
                )
            override val presentedOfferingIdentifier: String?
                get() = presentedOfferingContext?.offeringIdentifier
            override val presentedOfferingContext: PresentedOfferingContext?
                get() = presentedOfferingContext
            override val sku: String
                get() = productId

            override fun copyWithOfferingId(offeringId: String): StoreProduct = this
            override fun copyWithPresentedOfferingContext(presentedOfferingContext: PresentedOfferingContext?): StoreProduct =
                this
        }
    }

    override fun copyWithOfferingId(offeringId: String): StoreProduct {
        return copyWithPresentedOfferingContext(PresentedOfferingContext(offeringId))
    }
}

@SuppressWarnings("EmptyFunctionBlock")
fun stubSubscriptionOption(
    id: String,
    productId: String,
    duration: Period = Period(1, Period.Unit.MONTH, "P1M"),
    pricingPhases: List<PricingPhase> = listOf(stubPricingPhase(billingPeriod = duration)),
    presentedOfferingContext: PresentedOfferingContext? = null,
    installmentsInfo: InstallmentsInfo? = null,
    tags: List<String> = listOf("tag"),
): SubscriptionOption = object : SubscriptionOption {
    override val id: String
        get() = id
    override val pricingPhases: List<PricingPhase>
        get() = pricingPhases
    override val tags: List<String>
        get() = tags
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext?.offeringIdentifier
    override val presentedOfferingContext: PresentedOfferingContext?
        get() = presentedOfferingContext
    override val purchasingData: PurchasingData
        get() = StubPurchasingData(
            productId = productId,
        )
    override val installmentsInfo: InstallmentsInfo?
        get() = installmentsInfo
}

@SuppressWarnings("LongParameterList")
@JvmSynthetic
internal fun stubGoogleSubscriptionOption(
    productId: String,
    basePlanId: String,
    productDetails: ProductDetails,
    offerId: String? = null,
    pricingPhases: List<PricingPhase> = listOf(stubPricingPhase()),
    tags: List<String> = listOf("tag"),
    offerToken: String = "test_offer_token",
    presentedOfferingContext: PresentedOfferingContext? = null,
    installmentsInfo: GoogleInstallmentsInfo? = null,
): GoogleSubscriptionOption = GoogleSubscriptionOption(
    productId = productId,
    basePlanId = basePlanId,
    offerId = offerId,
    pricingPhases = pricingPhases,
    tags = tags,
    productDetails = productDetails,
    offerToken = offerToken,
    presentedOfferingContext = presentedOfferingContext,
    installmentsInfo = installmentsInfo
)

fun stubFreeTrialPricingPhase(
    billingPeriod: Period = Period(1, Period.Unit.MONTH, "P1M"),
    priceCurrencyCodeValue: String = "USD",
) = stubPricingPhase(
    billingPeriod = billingPeriod,
    priceCurrencyCodeValue = priceCurrencyCodeValue,
    price = 0.0,
    recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
    billingCycleCount = 1,
)

fun stubPricingPhase(
    billingPeriod: Period = Period(1, Period.Unit.MONTH, "P1M"),
    priceCurrencyCodeValue: String = "USD",
    price: Double = 4.99,
    recurrenceMode: Int = ProductDetails.RecurrenceMode.INFINITE_RECURRING,
    billingCycleCount: Int = 0,
): PricingPhase = PricingPhase(
    billingPeriod,
    recurrenceMode.toRecurrenceMode(),
    billingCycleCount,
    Price(
        if (price == 0.0) "Free" else "${'$'}$price",
        price.times(MICRO_MULTIPLIER).toLong(),
        priceCurrencyCodeValue,
    ),
)
