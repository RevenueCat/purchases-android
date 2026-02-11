package com.revenuecat.purchases.google

import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleReplacementMode

internal class PurchaseContext(
    public val productType: ProductType,
    public val presentedOfferingContext: PresentedOfferingContext?,
    public val selectedSubscriptionOptionId: String?,
    public val replacementMode: GoogleReplacementMode?,
    public val subscriptionOptionIdForProductIDs: Map<String, String>?,
)
