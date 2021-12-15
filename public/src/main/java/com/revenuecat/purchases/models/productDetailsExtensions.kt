@file:JvmName("StoreProductHelpers")

package com.revenuecat.purchases.models

import com.android.billingclient.api.SkuDetails

/**
 * Returns the original SkuDetails that was used to build the StoreProduct object.
 */
val StoreProduct.skuDetails: SkuDetails
    get() = SkuDetails(this.originalJson.toString())
