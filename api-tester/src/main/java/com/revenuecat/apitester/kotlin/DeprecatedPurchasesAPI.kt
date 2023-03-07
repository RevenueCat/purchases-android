package com.revenuecat.apitester.kotlin

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ProductChangeInfo
import com.revenuecat.purchases.getNonSubscriptionSkusWith
import com.revenuecat.purchases.getSubscriptionSkusWith
import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.purchasePackageWith
import com.revenuecat.purchases.purchaseProductWith
import com.revenuecat.purchases.purchaseSubscriptionOptionWith

@Suppress("unused")
private class DeprecatedPurchasesAPI {
    @SuppressWarnings("LongParameterList", "LongMethod", "EmptyFunctionBlock")
    fun check(
        purchases: Purchases,
        activity: Activity,
        storeProduct: StoreProduct,
        productChangeInfo: ProductChangeInfo,
        subscriptionOption: SubscriptionOption,
        packageToPurchase: com.revenuecat.purchases.Package
    ) {
        val purchaseChangeCallback = object : ProductChangeCallback {
            override fun onCompleted(storeTransaction: StoreTransaction?, customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError, userCancelled: Boolean) {}
        }
        val purchaseCallback = object : PurchaseCallback {
            override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError, userCancelled: Boolean) {}
        }

        purchases.purchaseProduct(activity, storeProduct, productChangeInfo, purchaseChangeCallback)
        purchases.purchaseProduct(activity, storeProduct, purchaseCallback)
        purchases.purchasePackage(activity, packageToPurchase, productChangeInfo, purchaseChangeCallback)
        purchases.purchasePackage(activity, packageToPurchase, purchaseCallback)

        purchases.purchaseSubscriptionOption(
            activity,
            subscriptionOption,
            productChangeInfo,
            purchaseChangeCallback
        )
        purchases.purchaseSubscriptionOption(activity, subscriptionOption, purchaseCallback)

        purchases.purchaseProductWith(
            activity,
            storeProduct,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction, _: CustomerInfo -> }
        )
        purchases.purchaseProductWith(
            activity,
            storeProduct,
            productChangeInfo,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction?, _: CustomerInfo -> }
        )
        purchases.purchasePackageWith(
            activity,
            packageToPurchase,
            productChangeInfo,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction?, _: CustomerInfo -> }
        )
        purchases.purchasePackageWith(
            activity,
            packageToPurchase,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction, _: CustomerInfo -> }
        )
        purchases.purchaseSubscriptionOptionWith(
            activity,
            subscriptionOption,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction, _: CustomerInfo -> }
        )
        purchases.purchaseSubscriptionOptionWith(
            activity,
            subscriptionOption,
            productChangeInfo,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction?, _: CustomerInfo -> }
        )

        purchases.allowSharingPlayStoreAccount = true
        purchases.getSubscriptionSkusWith(
            ArrayList<String>(),
            onError = { _: PurchasesError -> },
            onReceiveSkus = { _: List<StoreProduct> -> }
        )
        purchases.getNonSubscriptionSkusWith(
            ArrayList<String>(),
            onError = { _: PurchasesError -> },
            onReceiveSkus = { _: List<StoreProduct> -> }
        )

        Purchases.debugLogsEnabled = false
        val debugLogs: Boolean = Purchases.debugLogsEnabled
    }
}
