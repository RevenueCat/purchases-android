package com.revenuecat.purchases

import android.app.Activity
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.strings.PurchaseStrings
import dev.drewhamilton.poko.Poko

@Poko
class PurchaseParams(val builder: Builder) {

    val isPersonalizedPrice: Boolean?
    val oldProductId: String?
    val googleReplacementMode: GoogleReplacementMode

    @get:JvmSynthetic
    internal val purchasingData: PurchasingData

    @get:JvmSynthetic
    internal val activity: Activity

    @get:JvmSynthetic
    internal var presentedOfferingContext: PresentedOfferingContext?

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @get:JvmSynthetic
    internal val containsAddOnItems: Boolean
        get() = (purchasingData as? GooglePurchasingData.Subscription)
            ?.addOnProducts
            ?.isNotEmpty()
            ?: false

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
        @get:JvmSynthetic internal var purchasingData: PurchasingData,
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

        /*
         * The [Package]s to add on to the base package passed in via the [PurchaseParams.Builder]'s constructor.
         * This will result in a multi-line purchase whose base product is the one passed in to the
         * [PurchaseParams.Builder]'s constructor. The [defaultOption] for each add-on will be purchased.
         * [defaultOption] is selected via the following logic:
         *   - Filters out offers with "rc-ignore-offer" or "rc-customer-center" tag
         *   - Uses [SubscriptionOption] with the longest free trial or cheapest first phase
         *   - Falls back to use base plan
         *
         * The following restrictions apply to add-on purchases:
         * - Add-on purchases are currently only supported for subscriptions on the Play Store.
         * - The renewal periods of all add-on packages must be the same and match the period of the base product.
         * - No more than 49 add-ons packages per multi-line purchase are allowed.
         */
        @ExperimentalPreviewRevenueCatPurchasesAPI
        fun addOnPackages(addOnPackages: List<Package>) = apply {
            this.addOnStoreProducts(addOnPackages.map { it.product })
        }

        /*
         * The [StoreProduct]s to add on to the base product passed in via the [PurchaseParams.Builder]'s constructor.
         * This will result in a multi-line purchase whose base product is the one passed in to the
         * [PurchaseParams.Builder]'s constructor. The [defaultOption] for each add-on will be purchased.
         * [defaultOption] is selected via the following logic:
         *   - Filters out offers with "rc-ignore-offer" or "rc-customer-center" tag
         *   - Uses [SubscriptionOption] with the longest free trial or cheapest first phase
         *   - Falls back to use base plan
         *
         * The following restrictions apply to add-on purchases:
         * - Add-on purchases are currently only supported for subscriptions on the Play Store.
         * - The renewal periods of all add-on products must be the same and match the period of the base item.
         * - No more than 49 add-ons products per multi-line purchase are allowed.
         */
        @ExperimentalPreviewRevenueCatPurchasesAPI
        fun addOnStoreProducts(addOnStoreProducts: List<StoreProduct>) = apply {
            val compatibleAddOnProducts: List<GooglePurchasingData> = addOnStoreProducts
                .mapNotNull { it.purchasingData as? GooglePurchasingData.Subscription }

            attachSubscriptionAddOns(addOns = compatibleAddOnProducts)
        }

        /*
         * The [SubscriptionOption]s to add on to the base product passed in via
         * the [PurchaseParams.Builder]'s constructor. This will result in a multi-line purchase whose base product
         * is the one passed in to the [PurchaseParams.Builder]'s constructor.
         *
         * The following restrictions apply to add-on purchases:
         * - Add-on purchases are currently only supported for subscriptions on the Play Store.
         * - The renewal periods of all add-on products must be the same and match the period of the base item.
         * - No more than 49 add-ons products per multi-line purchase are allowed.
         */
        @ExperimentalPreviewRevenueCatPurchasesAPI
        fun addOnSubscriptionOptions(addOnSubscriptionOptions: List<SubscriptionOption>) = apply {
            val compatibleAddOnProducts: List<GooglePurchasingData> = addOnSubscriptionOptions
                .mapNotNull { it.purchasingData as? GooglePurchasingData.Subscription }

            attachSubscriptionAddOns(addOns = compatibleAddOnProducts)
        }

        @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
        private fun attachSubscriptionAddOns(addOns: List<GooglePurchasingData>) = apply {
            if (addOns.isEmpty()) {
                log(LogIntent.DEBUG) { PurchaseStrings.EMPTY_ADD_ONS_LIST_PASSED }
                return@apply
            }

            val existingPurchasingData = this.purchasingData as? GooglePurchasingData.Subscription
            existingPurchasingData?.let {
                val compatibleAddOnProducts: List<GooglePurchasingData.Subscription> = addOns
                    .mapNotNull { it as? GooglePurchasingData.Subscription }

                val newPurchasingData = GooglePurchasingData.Subscription(
                    productId = existingPurchasingData.productId,
                    optionId = existingPurchasingData.optionId,
                    productDetails = existingPurchasingData.productDetails,
                    token = existingPurchasingData.token,
                    billingPeriod = existingPurchasingData.billingPeriod,
                    addOnProducts = (existingPurchasingData.addOnProducts ?: emptyList()) + compatibleAddOnProducts,
                )

                this.purchasingData = newPurchasingData
            }
        }

        open fun build(): PurchaseParams {
            return PurchaseParams(this)
        }
    }
}
