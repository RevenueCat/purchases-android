package com.revenuecat.purchases.samsung

import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import dev.drewhamilton.poko.Poko

@Poko
class SamsungStoreProduct(

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
     * Name of the product. This will match the name for Samsung products.
     */
    override val name: String,

    /**
     * Name of the product. This will match the name for Samsung products.
     */
    override val title: String,

    /**
     * Name of the product. This will match the item description for Samsung products.
     */
    override val description: String,

    /**
     * Subscription period.
     *
     * Note: Returned only for subscriptions. Null for INAPP products.
     */
    override val period: Period?,

    /**
     * Always null for SamsungStoreProduct
     */
    override val subscriptionOptions: SubscriptionOptions?,

    /**
     * Always null for SamsungStoreProduct
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
        return SamsungStoreProduct(
            id = this.id,
            type = this.type,
            price = this.price,
            name = this.name,
            title = this.title,
            description = this.description,
            period = this.period,
            subscriptionOptions = this.subscriptionOptions,
            defaultOption = this.defaultOption,
            presentedOfferingContext = presentedOfferingContext,
        )
    }

    /**
     * Contains only data that is required to make the purchase.
     */
    override val purchasingData: SamsungPurchasingData
        get() = SamsungPurchasingData.Product(this)

    /**
     * The offering ID this `SamsungStoreProduct` was returned from.
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
