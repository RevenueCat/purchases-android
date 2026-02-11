package com.revenuecat.purchases.models

import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.simulatedstore.SimulatedStorePurchasingData
import dev.drewhamilton.poko.Poko

/**
 * A test-only [StoreProduct] implementation.
 * This can be used to create mock data for tests or Jetpack Compose previews.
 */

@Poko
public class TestStoreProduct @JvmOverloads constructor(
    override val id: String,
    override val name: String,
    override val title: String,
    override val description: String,
    override val price: Price,
    override val period: Period? = null,
    private val freeTrialPricingPhase: PricingPhase? = null,
    private val introPricePricingPhase: PricingPhase? = null,
    override val presentedOfferingContext: PresentedOfferingContext? = null,
) : StoreProduct {

    @Deprecated(
        "Replaced with constructor that takes pricing phases for free trial and intro price",
        ReplaceWith(
            "TestStoreProduct(id, name, title, description, price, period, " +
                "freeTrialPricingPhase, introPricePricingPhase)",
        ),
    )
    constructor(
        id: String,
        name: String,
        title: String,
        description: String,
        price: Price,
        period: Period? = null,
        freeTrialPeriod: Period? = null,
        introPrice: Price? = null,
    ) : this(
        id,
        name,
        title,
        description,
        price,
        period,
        freeTrialPeriod?.let {
            PricingPhase(
                billingPeriod = it,
                recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
                price = Price(amountMicros = 0, currencyCode = price.currencyCode, formatted = "Free"),
            )
        },
        introPrice?.let {
            PricingPhase(
                billingPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
                recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
                price = it,
            )
        },
    )

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
    override val purchasingData: PurchasingData = SimulatedStorePurchasingData(
        productId = id,
        productType = type,
        storeProduct = this,
    )

    @Deprecated(
        "Use presentedOfferingContext",
        ReplaceWith("presentedOfferingContext.offeringIdentifier"),
    )
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext?.offeringIdentifier
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
        return TestStoreProduct(
            id = id,
            name = name,
            title = title,
            description = description,
            price = price,
            period = period,
            freeTrialPricingPhase = freeTrialPricingPhase,
            introPricePricingPhase = introPricePricingPhase,
            presentedOfferingContext = presentedOfferingContext,
        )
    }

    private fun buildSubscriptionOptions(): SubscriptionOptions? {
        if (period == null) return null
        val basePricePhase = PricingPhase(
            billingPeriod = period,
            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
            billingCycleCount = null,
            price = price,
        )
        val subscriptionOptionsList = listOfNotNull(
            TestSubscriptionOption(
                listOfNotNull(freeTrialPricingPhase, introPricePricingPhase, basePricePhase),
                purchasingData = purchasingData,
            ).takeIf { freeTrialPricingPhase != null || introPricePricingPhase != null },
            TestSubscriptionOption(
                listOf(basePricePhase),
                purchasingData = purchasingData,
            ),
        )
        return SubscriptionOptions(subscriptionOptionsList)
    }
}

private class TestSubscriptionOption(
    override val pricingPhases: List<PricingPhase>,
    val basePlanId: String = "testBasePlanId",
    override val tags: List<String> = emptyList(),
    override val presentedOfferingContext: PresentedOfferingContext = PresentedOfferingContext(
        offeringIdentifier = "offering",
    ),
    override val installmentsInfo: InstallmentsInfo? = null,
    override val purchasingData: PurchasingData,
) : SubscriptionOption {
    override val id: String
        get() = if (pricingPhases.size == 1) basePlanId else "$basePlanId:testOfferId"

    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext.offeringIdentifier
}
