package com.revenuecat.purchases.google

import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.StoreReplacementMode
import java.util.Date

internal class PurchaseContext(
    val productType: ProductType,
    val presentedOfferingContext: PresentedOfferingContext?,
    val selectedSubscriptionOptionId: String?,
    val replacementMode: StoreReplacementMode?,
    val subscriptionOptionIdForProductIDs: Map<String, String>?,
    val initiationDate: Date,
)
