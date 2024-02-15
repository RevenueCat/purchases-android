package com.revenuecat.purchases.models

import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType

/**
 * A test-only [StoreProduct] implementation.
 * This can be used to create mock data for tests or Jetpack Compose previews.
 */
data class TestStoreProduct(
    override val id: String,
    override val name: String,
    override val title: String,
    override val description: String,
    override val price: Price,
    override val period: Period?,
    private val freeTrialPeriod: Period? = null,
    private val introPrice: Price? = null,
) : StoreProduct {
    @Deprecated(
        "Replaced with constructor that takes a name",
        ReplaceWith(
            "TestStoreProduct(id, name, title, description, price, period, " +
                "freeTrialPeriod, introPrice)",
        ),
    )
    constructor(
        id: String,
        title: String,
        description: String,
        price: Price,
        period: Period?,
        freeTrialPeriod: Period? = null,
        introPrice: Price? = null,
    ) : this(
        id,
        title,
        title,
        description,
        price,
        period,
        freeTrialPeriod,
        introPrice,
    )
    override val type: ProductType
        get() = if (period == null) ProductType.INAPP else ProductType.SUBS
    override val subscriptionOptions: SubscriptionOptions?
        get() = buildSubscriptionOptions()
    override val defaultOption: SubscriptionOption?
        get() = subscriptionOptions?.defaultOffer
    override val purchasingData: PurchasingData
        get() = object : PurchasingData {
            override val productId: String
                get() = id
            override val productType: ProductType
                get() = type
        }

    @Deprecated(
        "Use presentedOfferingContext",
        ReplaceWith("presentedOfferingContext.offeringIdentifier"),
    )
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext?.offeringIdentifier
    override val presentedOfferingContext: PresentedOfferingContext?
        get() = null
    override val sku: String
        get() = id

    @Deprecated(
        "Use copyWithPresentedOfferingContext instead",
        ReplaceWith("copyWithPresentedOfferingContext(PresentedOfferingContext(offeringId))"),
    )
    override fun copyWithOfferingId(offeringId: String): StoreProduct {
        return copyWithPresentedOfferingContext(PresentedOfferingContext(offeringId))
    }

    override fun copyWithPresentedOfferingContext(presentedOfferingContext: PresentedOfferingContext?): StoreProduct {
        return this
    }

    private fun buildSubscriptionOptions(): SubscriptionOptions? {
        if (period == null) return null
        val freePhase = freeTrialPeriod?.let { freeTrialPeriod ->
            PricingPhase(
                billingPeriod = freeTrialPeriod,
                recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
                price = Price(amountMicros = 0, currencyCode = price.currencyCode, formatted = "Free"),
            )
        }
        val introPhase = introPrice?.let { introPrice ->
            PricingPhase(
                billingPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
                recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
                price = introPrice,
            )
        }
        val basePricePhase = PricingPhase(
            billingPeriod = period,
            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
            billingCycleCount = null,
            price = price,
        )
        val subscriptionOptionsList = listOfNotNull(
            TestSubscriptionOption(
                id,
                listOfNotNull(freePhase, introPhase, basePricePhase),
            ).takeIf { freeTrialPeriod != null || introPhase != null },
            TestSubscriptionOption(
                id,
                listOf(basePricePhase),
            ),
        )
        return SubscriptionOptions(subscriptionOptionsList)
    }
}

private class TestSubscriptionOption(
    val productIdentifier: String,
    override val pricingPhases: List<PricingPhase>,
    val basePlanId: String = "testBasePlanId",
    override val tags: List<String> = emptyList(),
    override val presentedOfferingContext: PresentedOfferingContext = PresentedOfferingContext(
        offeringIdentifier = "offering",
    ),
) : SubscriptionOption {
    override val id: String
        get() = if (pricingPhases.size == 1) basePlanId else "$basePlanId:testOfferId"

    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext.offeringIdentifier

    override val purchasingData: PurchasingData
        get() = object : PurchasingData {
            override val productId: String
                get() = productIdentifier
            override val productType: ProductType
                get() = ProductType.SUBS
        }
}
