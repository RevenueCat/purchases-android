package com.revenuecat.purchases

import android.app.Activity
import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption

open class Purchase(builder: Builder) {

    val isPersonalizedPrice: Boolean
    val oldProductId: String?
    val googleProrationMode: GoogleProrationMode
    internal val purchasingData: PurchasingData
    internal val activity: Activity
    internal val listener: ProductChangeCallback

    init {
        this.isPersonalizedPrice = builder.isPersonalizedPrice
        this.oldProductId = builder.oldProductId
        this.googleProrationMode = builder.googleProrationMode
        this.purchasingData = builder.purchasingData
        this.activity = builder.activity
        this.listener = builder.listener
    }

    open class Builder private constructor(
        @get:JvmSynthetic internal val purchasingData: PurchasingData,
        @get:JvmSynthetic internal val activity: Activity,
        @get:JvmSynthetic internal val listener: ProductChangeCallback
    ) {
        // TODO jvmsynthetic?
        // TODO see if there's a way to clean up the purchasingData logic
        constructor(packageToPurchase: Package, activity: Activity, listener: ProductChangeCallback) :
            this(
                packageToPurchase.product.defaultOption?.purchasingData ?: packageToPurchase.product.purchasingData,
                activity,
                listener
            )

        constructor(
            storeProduct: StoreProduct,
            activity: Activity,
            listener: ProductChangeCallback
        ) : this(storeProduct.defaultOption?.purchasingData ?: storeProduct.purchasingData, activity, listener)

        constructor(subscriptionOption: SubscriptionOption, activity: Activity, listener: ProductChangeCallback) : this(
            subscriptionOption.purchasingData,
            activity,
            listener
        )

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var isPersonalizedPrice: Boolean = false

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var oldProductId: String? = null

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var googleProrationMode: GoogleProrationMode = GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION

        fun isPersonalizedPrice(isPersonalizedPrice: Boolean) = apply {
            this.isPersonalizedPrice = isPersonalizedPrice
        }

        fun oldProductId(oldProductId: String?) = apply {
            this.oldProductId = oldProductId
        }

        fun googleProrationMode(googleProrationMode: GoogleProrationMode) = apply {
            this.googleProrationMode = googleProrationMode
        }

        open fun build(): Purchase {
            return Purchase(this)
        }
    }
}
