package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import dev.drewhamilton.poko.Poko

public sealed class GooglePurchasingData : PurchasingData {
    @Poko
    public class InAppProduct(
        public override val productId: String,
        public val productDetails: ProductDetails,
    ) : GooglePurchasingData()

    @Poko
    public class Subscription(
        public override val productId: String,
        public val optionId: String,
        public val productDetails: ProductDetails,
        public val token: String,
    ) : GooglePurchasingData()

    public override val productType: ProductType
        get() = when (this) {
            is InAppProduct -> {
                ProductType.INAPP
            }
            is Subscription -> {
                ProductType.SUBS
            }
        }
}
