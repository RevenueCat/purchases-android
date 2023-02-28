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

    init {
        this.isPersonalizedPrice = builder.isPersonalizedPrice
        this.oldProductId = builder.oldProductId
        this.googleProrationMode = builder.googleProrationMode
        this.purchasingData = builder.purchasingData
        this.activity = builder.activity
    }

    open class Builder private constructor(
        @get:JvmSynthetic internal val purchasingData: PurchasingData,
        @get:JvmSynthetic internal val activity: Activity,
    ) {
        // TODO jvmsynthetic?
        // TODO see if there's a way to clean up the purchasingData logic
        constructor(packageToPurchase: Package, activity: Activity) :
            this(
                packageToPurchase.product.defaultOption?.purchasingData ?: packageToPurchase.product.purchasingData,
                activity
            )

        constructor(
            storeProduct: StoreProduct,
            activity: Activity
        ) : this(storeProduct.defaultOption?.purchasingData ?: storeProduct.purchasingData, activity)

        constructor(subscriptionOption: SubscriptionOption, activity: Activity) :
            this(
                subscriptionOption.purchasingData,
                activity
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
