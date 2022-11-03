package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.ReplaceSkuInfo
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.sha256
import com.revenuecat.purchases.models.GooglePurchaseOption
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.strings.PurchaseStrings

fun @receiver:BillingClient.ProductType String.buildQueryPurchaseHistoryParams(): QueryPurchaseHistoryParams? {
    return when (this) {
        BillingClient.ProductType.INAPP,
        BillingClient.ProductType.SUBS -> QueryPurchaseHistoryParams.newBuilder().setProductType(this).build()
        else -> null
    }
}

fun @receiver:BillingClient.ProductType String.buildQueryPurchasesParams(): QueryPurchasesParams? {
    return when (this) {
        BillingClient.ProductType.INAPP,
        BillingClient.ProductType.SUBS -> QueryPurchasesParams.newBuilder().setProductType(this).build()
        else -> null
    }
}

fun @receiver:BillingClient.ProductType String.buildQueryProductDetailsParams(
    productIds: Set<String>
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

// TODO move somewhere
val SubscriptionOfferDetails.subscriptionBillingPeriod: String?
    get() = this.pricingPhases.pricingPhaseList.lastOrNull()?.billingPeriod

val SubscriptionOfferDetails.isBasePlan: Boolean
    get() = this.pricingPhases.pricingPhaseList.size == 1