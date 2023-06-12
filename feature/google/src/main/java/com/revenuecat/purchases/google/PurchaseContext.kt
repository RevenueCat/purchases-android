package com.revenuecat.purchases.google

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleReplacementMode

class PurchaseContext(
    val productType: ProductType,
    val presentedOfferingId: String?,
    val selectedSubscriptionOptionId: String?,
    val replacementMode: GoogleReplacementMode?,
)
