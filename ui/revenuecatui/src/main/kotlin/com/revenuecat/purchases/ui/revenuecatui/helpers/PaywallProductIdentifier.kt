@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.StoreProduct

/**
 * Uses canonical Play product ID for Google subscriptions.
 */
internal fun StoreProduct.paywallProductIdentifier(): String {
    return if (this is GoogleStoreProduct) {
        this.productId
    } else {
        this.id
    }
}
