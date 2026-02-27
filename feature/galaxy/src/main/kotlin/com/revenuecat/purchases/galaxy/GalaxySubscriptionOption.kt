package com.revenuecat.purchases.galaxy

import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.models.InstallmentsInfo
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.SubscriptionOption
import dev.drewhamilton.poko.Poko

@Poko
public class GalaxySubscriptionOption(
    /**
     * The product ID of the Galaxy Store product represented by this option.
     */
    override val id: String,

    /**
     * Pricing phases defining a user's payment plan for the product over time.
     */
    override val pricingPhases: List<PricingPhase>,

    /**
     * Always empty for Galaxy products.
     */
    override val tags: List<String>,

    /**
     * The context from which this subscription option was obtained.
     *
     * Null if not using RevenueCat offerings system, if fetched directly via `Purchases.getProducts`,
     * or on restores/syncs.
     */
    override val presentedOfferingContext: PresentedOfferingContext?,
    override val purchasingData: PurchasingData,

    /**
     * Always null.
     */
    override val installmentsInfo: InstallmentsInfo?,
) : SubscriptionOption {

    internal constructor(
        subscriptionOption: GalaxySubscriptionOption,
        presentedOfferingContext: PresentedOfferingContext?,
    ) :
        this(
            id = subscriptionOption.id,
            pricingPhases = subscriptionOption.pricingPhases,
            tags = subscriptionOption.tags,
            presentedOfferingContext = presentedOfferingContext,
            purchasingData = subscriptionOption.purchasingData,
            installmentsInfo = subscriptionOption.installmentsInfo,
        )

    /**
     * The offering ID this `GalaxySubscriptionOption` was returned from.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `Purchases.getProducts`
     */
    @Deprecated(
        "Use presentedOfferingContext instead",
        ReplaceWith("presentedOfferingContext.offeringIdentifier"),
    )
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext?.offeringIdentifier
}
