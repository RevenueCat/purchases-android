package com.revenuecat.purchases.google

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleProrationMode

class PurchaseContext(
    val productType: ProductType,
    val presentedOffering: String?,
    val subscriptionOptionSelected: String?,
    val prorationMode: GoogleProrationMode?
)
