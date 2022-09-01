package com.revenuecat.purchases.google

import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams

fun String.buildQueryPurchaseHistoryParams(): QueryPurchaseHistoryParams {
    return QueryPurchaseHistoryParams.newBuilder().setProductType(this).build()
}

fun String.buildQueryPurchasesParams(): QueryPurchasesParams {
    return QueryPurchasesParams.newBuilder().setProductType(this).build()
}
