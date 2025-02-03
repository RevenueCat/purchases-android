@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.utils

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.InstallmentsInfo
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.toRecurrenceMode

@SuppressWarnings("LongParameterList")
@JvmSynthetic
internal fun previewSubscriptionOption(
    id: String,
    productId: String,
    duration: Period = Period(1, Period.Unit.MONTH, "P1M"),
    pricingPhases: List<PricingPhase> = listOf(previewPricingPhase(billingPeriod = duration)),
    presentedOfferingContext: PresentedOfferingContext? = null,
    installmentsInfo: InstallmentsInfo? = null,
): SubscriptionOption = object : SubscriptionOption {
    override val id: String
        get() = id
    override val pricingPhases: List<PricingPhase>
        get() = pricingPhases
    override val tags: List<String>
        get() = listOf("tag")
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext?.offeringIdentifier
    override val presentedOfferingContext: PresentedOfferingContext?
        get() = presentedOfferingContext
    override val purchasingData: PurchasingData
        get() = PreviewPurchasingData(
            productId = productId,
        )
    override val installmentsInfo: InstallmentsInfo?
        get() = installmentsInfo
}

@SuppressWarnings("MagicNumber")
@JvmSynthetic
internal fun previewPricingPhase(
    billingPeriod: Period = Period(1, Period.Unit.MONTH, "P1M"),
    priceCurrencyCodeValue: String = "USD",
    price: Double = 4.99,
    recurrenceMode: Int = ProductDetails.RecurrenceMode.INFINITE_RECURRING,
    billingCycleCount: Int? = null,
): PricingPhase = PricingPhase(
    billingPeriod,
    recurrenceMode.toRecurrenceMode(),
    billingCycleCount,
    Price(
        if (price == 0.0) "Free" else "${'$'}$price",
        price.times(1_000_000).toLong(),
        priceCurrencyCodeValue,
    ),
)

internal data class PreviewPurchasingData(
    @get:JvmSynthetic override val productId: String,
) : PurchasingData {

    @get:JvmSynthetic
    override val productType: ProductType
        get() = ProductType.SUBS
}
