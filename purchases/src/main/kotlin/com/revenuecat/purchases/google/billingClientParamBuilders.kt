package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams

internal fun @receiver:BillingClient.ProductType String.buildQueryPurchaseHistoryParams(): QueryPurchaseHistoryParams? {
    return when (this) {
        BillingClient.ProductType.INAPP,
        BillingClient.ProductType.SUBS,
        -> QueryPurchaseHistoryParams.newBuilder().setProductType(this).build()
        else -> null
    }
}

internal fun @receiver:BillingClient.ProductType String.buildQueryPurchasesParams(): QueryPurchasesParams? {
    return when (this) {
        BillingClient.ProductType.INAPP,
        BillingClient.ProductType.SUBS,
        -> QueryPurchasesParams.newBuilder().setProductType(this).build()
        else -> null
    }
}

internal fun @receiver:BillingClient.ProductType String.buildQueryProductDetailsParams(
    productIds: Set<String>,
): QueryProductDetailsParams {
    val productList = productIds.map { productId ->
        QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(this)
            .build()
    }
    return QueryProductDetailsParams.newBuilder()
        .setProductList(productList).build()
}
