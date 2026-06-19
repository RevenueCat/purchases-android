package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback

@InternalRevenueCatAPI
@Suppress("ForbiddenPublicDataClass")
public data class PurchasesState(
    public val allowSharingPlayStoreAccount: Boolean? = null,
    public val purchaseCallbacksByProductId: Map<String, PurchaseCallback> = emptyMap(),
    public val deprecatedProductChangeCallback: ProductChangeCallback? = null,
    public val appInBackground: Boolean = true,
    public val firstTimeInForeground: Boolean = true,
)
