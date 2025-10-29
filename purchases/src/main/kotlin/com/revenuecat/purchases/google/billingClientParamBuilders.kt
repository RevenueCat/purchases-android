package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.revenuecat.purchases.common.errorLog

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

    return try {
        QueryProductDetailsParams.newBuilder()
            .setProductList(productList).build()
    } catch (@Suppress("SwallowedException") e: ExceptionInInitializerError) {
        // We've received reports that setProductList may throw an exception in some Chromebook devices
        // This is a workaround to avoid the crash and return a proper error to the developer.
        val errorMessage = "Error while building QueryProductDetailsParams in Billing client"
        errorLog(e) { "$errorMessage: ${e.message}. Caused by: ${e.cause?.message}" }
        throw QueryProductDetailsParamsBuilderException(
            errorMessage,
            e.cause,
        )
    }
}
