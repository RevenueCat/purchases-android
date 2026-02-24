package com.revenuecat.purchases.galaxy

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import dev.drewhamilton.poko.Poko

@Poko
public class GalaxyStoreProduct(

    /**
     * The productId for this product.
     */
    override val id: String,

    /**
     * Type of product. One of [ProductType].
     */
    override val type: ProductType,

    /**
     * Price information for a non-subscription product.
     * Term price for a subscription.
     */
    override val price: Price,

    /**
     * Name of the product. This will match the name for Galaxy products.
     */
    override val name: String,

    /**
     * Name of the product. This will match the name for Galaxy products.
     */
    override val title: String,

    /**
     * Name of the product. This will match the item description for Galaxy products.
     */
    override val description: String,

    /**
     * Subscription period.
     *
     * Note: Returned only for subscriptions. Null for INAPP products.
     */
    override val period: Period?,

    /**
     * The subscription options for this product.
     */
    override val subscriptionOptions: SubscriptionOptions?,

    /**
     * The default subscription option for this product.
     */
    override val defaultOption: SubscriptionOption?,

    /**
     * The context from which this product was obtained.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `Purchases.getProducts`
     */
    override val presentedOfferingContext: PresentedOfferingContext?,
) : StoreProduct {

    /**
     * For internal RevenueCat use.
     *
     * Creates a copy of this `AmazonStoreProduct` with the specified `offeringId` set.
     */
    @OptIn(InternalRevenueCatAPI::class)
    @Deprecated(
        "Replaced with copyWithPresentedOfferingContext",
        ReplaceWith("copyWithPresentedOfferingContext(PresentedOfferingContext(offeringId))"),
    )
    override fun copyWithOfferingId(offeringId: String): StoreProduct {
        val newPresentedOfferingContext = presentedOfferingContext?.copy(offeringIdentifier = offeringId)
            ?: PresentedOfferingContext(offeringId)
        return copyWithPresentedOfferingContext(newPresentedOfferingContext)
    }

    override fun copyWithPresentedOfferingContext(presentedOfferingContext: PresentedOfferingContext?): StoreProduct {
        val subscriptionOptionsWithContext = subscriptionOptions?.mapNotNull {
            (it as? GalaxySubscriptionOption)?.let { galaxyOption ->
                GalaxySubscriptionOption(
                    subscriptionOption = galaxyOption,
                    presentedOfferingContext = presentedOfferingContext,
                )
            }
        }

        val defaultOptionWithContext = subscriptionOptionsWithContext?.firstOrNull()

        return GalaxyStoreProduct(
            id = this.id,
            type = this.type,
            price = this.price,
            name = this.name,
            title = this.title,
            description = this.description,
            period = this.period,
            subscriptionOptions = subscriptionOptionsWithContext?.let { SubscriptionOptions(it) },
            defaultOption = defaultOptionWithContext,
            presentedOfferingContext = presentedOfferingContext,
        )
    }

    /**
     * Contains only data that is required to make the purchase.
     */
    override val purchasingData: GalaxyPurchasingData
        get() = GalaxyPurchasingData.Product(
            productId = id,
            productType = type,
        )

    /**
     * The offering ID this `GalaxyStoreProduct` was returned from.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `Purchases.getProducts`
     */
    @Deprecated(
        "Use presentedOfferingContext instead",
        ReplaceWith("presentedOfferingContext.offeringIdentifier"),
    )
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext?.offeringIdentifier

    @Deprecated(
        "Replaced with id",
        ReplaceWith("id"),
    )
    override val sku: String
        get() = id
}
