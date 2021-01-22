package com.revenuecat.purchases.models

import com.android.billingclient.api.SkuDetails

val ProductDetails.skuDetails: SkuDetails
    get() = SkuDetails(this.originalJson.toString())
