@file:JvmName("ProductDetailsHelpers")

package com.revenuecat.purchases.models

import com.android.billingclient.api.SkuDetails

/**
 * Returns the original SkuDetails that was used to build the ProductDetails object.
 */
val StoreProduct.skuDetails: SkuDetails
    get() = SkuDetails(this.originalJson.toString())
