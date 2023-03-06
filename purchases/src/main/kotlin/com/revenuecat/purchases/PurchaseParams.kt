package com.revenuecat.purchases

import android.app.Activity
import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption

open class PurchaseParams(builder: Builder) {

    val isPersonalizedPrice: Boolean
    val oldProductId: String?
    val googleProrationMode: GoogleProrationMode
    @get:JvmSynthetic
    internal val purchasingData: PurchasingData
    @get:JvmSynthetic
    internal val activity: Activity
    @get:JvmSynthetic
    internal val presentedOfferingIdentifier: String?

    init {
        this.isPersonalizedPrice = builder.isPersonalizedPrice
        this.oldProductId = builder.oldProductId
        this.googleProrationMode = builder.googleProrationMode
        this.purchasingData = builder.purchasingData
        this.activity = builder.activity
        this.presentedOfferingIdentifier = builder.presentedOfferingIdentifier
    }

    /**
     * Builder to configure a purchase.
     * Initialized with an [Activity] either a [Package], [StoreProduct], or [SubscriptionOption].
     *
     * If a [Package] or [StoreProduct] is passed in, the [defaultOption] will be purchased. [defaultOption] is
     * selected via the following logic:
     *   - Filters out offers with "rc-ignore-default-offer" tag
     *   - Uses [SubscriptionOption] with the longest free trial or cheapest first phase
     *   - Falls back to use base plan
     */
    open class Builder private constructor(
        @get:JvmSynthetic internal val purchasingData: PurchasingData,
        @get:JvmSynthetic internal val activity: Activity,
        @get:JvmSynthetic internal val presentedOfferingIdentifier: String? = null
    ) {
        constructor(packageToPurchase: Package, activity: Activity) :
            this(
                packageToPurchase.product.defaultOption?.purchasingData ?: packageToPurchase.product.purchasingData,
                activity,
                packageToPurchase.offering
            )

        constructor(storeProduct: StoreProduct, activity: Activity) :
            this(storeProduct.defaultOption?.purchasingData ?: storeProduct.purchasingData, activity)

        constructor(subscriptionOption: SubscriptionOption, activity: Activity) :
            this(subscriptionOption.purchasingData, activity)

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var isPersonalizedPrice: Boolean = false

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var oldProductId: String? = null

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var googleProrationMode: GoogleProrationMode = GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION

        /*
         * Indicates personalized pricing on products available for purchase in the EU.
         * For compliance with EU regulations. User will see "This price has been customize for you" in the purchase
         * dialog when true. See https://developer.android.com/google/play/billing/integrate#personalized-price
         * for more info.
         *
         * Default is false.
         * Ignored for Amazon Appstore purchases.
         */
        fun isPersonalizedPrice(isPersonalizedPrice: Boolean) = apply {
            this.isPersonalizedPrice = isPersonalizedPrice
        }

        /*
         * The product ID of the product to change from when initiating a product change.
         *
         * Product changes are only available in the Play Store. Ignored for Amazon Appstore purchases.
         */
        fun oldProductId(oldProductId: String?) = apply {
            this.oldProductId = oldProductId
        }

        /*
         * The [GoogleProrationMode] to use when upgrading the given oldProductId. Defaults to
         * [GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION].
         *
         * Only applied for Play Store product changes. Ignored for Amazon Appstore purchases.
         */
        fun googleProrationMode(googleProrationMode: GoogleProrationMode) = apply {
            this.googleProrationMode = googleProrationMode
        }

        open fun build(): PurchaseParams {
            return PurchaseParams(this)
        }
    }
}
