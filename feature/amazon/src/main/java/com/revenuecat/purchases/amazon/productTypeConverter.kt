package com.revenuecat.purchases.amazon

import com.amazon.device.iap.model.ProductType as AmazonProductType
import com.revenuecat.purchases.ProductType as RevenueCatProductType

fun AmazonProductType.toRevenueCatProductType(): RevenueCatProductType {
    return when (this) {
        AmazonProductType.CONSUMABLE -> RevenueCatProductType.INAPP
        AmazonProductType.ENTITLED -> RevenueCatProductType.INAPP
        AmazonProductType.SUBSCRIPTION -> RevenueCatProductType.SUBS
    }
}
