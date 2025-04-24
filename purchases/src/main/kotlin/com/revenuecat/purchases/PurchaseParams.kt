package com.revenuecat.purchases

import android.app.Activity
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.TestStoreProduct

data class PurchaseParams(val builder: Builder) {

    val isPersonalizedPrice: Boolean?
    val oldProductId: String?
    val googleReplacementMode: GoogleReplacementMode

    @get:JvmSynthetic
    internal val purchasingData: PurchasingData

    @get:JvmSynthetic
    internal val activity: Activity

    @get:JvmSynthetic
    internal var presentedOfferingContext: PresentedOfferingContext?

    init {
        this.isPersonalizedPrice = builder.isPersonalizedPrice
        this.oldProductId = builder.oldProductId
        this.googleReplacementMode = builder.googleReplacementMode
        this.purchasingData = builder.purchasingData
        this.activity = builder.activity
        this.presentedOfferingContext = builder.presentedOfferingContext
    }

    /**
     * Builder to configure a purchase.
     * Initialized with an [Activity] either a [Package], [StoreProduct], or [SubscriptionOption].
     *
     * If a [Package] or [StoreProduct] is passed in, the [defaultOption] will be purchased. [defaultOption] is
     * selected via the following logic:
     *   - Filters out offers with "rc-ignore-offer" or "rc-customer-center" tag
     *   - Uses [SubscriptionOption] with the longest free trial or cheapest first phase
     *   - Falls back to use base plan
     */
    open class Builder private constructor(
        @get:JvmSynthetic internal val activity: Activity,
        @get:JvmSynthetic internal val purchasingData: PurchasingData,
        @get:JvmSynthetic internal var presentedOfferingContext: PresentedOfferingContext?,
        @get:JvmSynthetic internal val product: StoreProduct?,
    ) {
        constructor(activity: Activity, packageToPurchase: Package) :
            this(
                activity,
                packageToPurchase.product.purchasingData,
                packageToPurchase.presentedOfferingContext,
                packageToPurchase.product,
            )

        constructor(activity: Activity, storeProduct: StoreProduct) :
            this(activity, storeProduct.purchasingData, storeProduct.presentedOfferingContext, storeProduct)

        private fun ensureNoTestProduct(storeProduct: StoreProduct) {
            if (storeProduct is TestStoreProduct) {
                throw PurchasesException(
                    PurchasesError(
                        PurchasesErrorCode.ProductNotAvailableForPurchaseError,
                        "Cannot purchase $storeProduct",
                    ),
                )
            }
        }

        constructor(activity: Activity, subscriptionOption: SubscriptionOption) :
            this(
                activity,
                subscriptionOption.purchasingData,
                subscriptionOption.presentedOfferingContext,
                product = null,
            )

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var isPersonalizedPrice: Boolean? = null

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var oldProductId: String? = null

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var googleReplacementMode: GoogleReplacementMode = GoogleReplacementMode.WITHOUT_PRORATION

        /*
         * Sets the data about the context in which an offering was presented.
         *
         * Default is set from the Package, StoreProduct, or SubscriptionOption used in the constructor.
         */
        fun presentedOfferingContext(presentedOfferingContext: PresentedOfferingContext) = apply {
            this.presentedOfferingContext = presentedOfferingContext
        }

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
         * The product ID of the product to change from when initiating a product change. We expect the subscriptionId
         * only. If a string including `:` is passed in, we will assume the string is in the form
         * `productId:basePlanId` and anything after the `:` will be ignored.
         *
         * Product changes are only available in the Play Store. Ignored for Amazon Appstore purchases.
         */
        fun oldProductId(oldProductId: String) = apply {
            this.oldProductId = oldProductId
        }

        /*
         * The [GoogleReplacementMode] to use when replacing the given oldProductId. Defaults to
         * [GoogleReplacementMode.WITHOUT_PRORATION].
         *
         * Only applied for Play Store product changes. Ignored for Amazon Appstore purchases.
         */
        fun googleReplacementMode(googleReplacementMode: GoogleReplacementMode) = apply {
            this.googleReplacementMode = googleReplacementMode
        }

        open fun build(): PurchaseParams {
            product?.let {
                ensureNoTestProduct(it)
            }

            return PurchaseParams(this)
        }
    }
}
