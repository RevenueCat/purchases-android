package com.revenuecat.apitester.kotlin

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.UpgradeInfo
import com.revenuecat.purchases.getNonSubscriptionSkusWith
import com.revenuecat.purchases.getSubscriptionSkusWith
import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.purchasePackageWith
import com.revenuecat.purchases.purchaseProductWith

@Suppress("unused")
private class DeprecatedPurchasesAPI {
    @SuppressWarnings("LongParameterList", "LongMethod", "EmptyFunctionBlock")
    fun check(
        purchases: Purchases,
        activity: Activity,
        storeProduct: StoreProduct,
        upgradeInfo: UpgradeInfo,
        subscriptionOption: SubscriptionOption,
        packageToPurchase: com.revenuecat.purchases.Package,
    ) {
        val purchaseChangeCallback = object : ProductChangeCallback {
            override fun onCompleted(storeTransaction: StoreTransaction?, customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError, userCancelled: Boolean) {}
        }
        val purchaseCallback = object : PurchaseCallback {
            override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError, userCancelled: Boolean) {}
        }

        purchases.purchaseProduct(activity, storeProduct, upgradeInfo, purchaseChangeCallback)
        purchases.purchaseProduct(activity, storeProduct, purchaseCallback)
        purchases.purchasePackage(activity, packageToPurchase, upgradeInfo, purchaseChangeCallback)
        purchases.purchasePackage(activity, packageToPurchase, purchaseCallback)

        purchases.purchaseProductWith(
            activity,
            storeProduct,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction, _: CustomerInfo -> },
        )
        purchases.purchaseProductWith(
            activity,
            storeProduct,
        ) { _: StoreTransaction, _: CustomerInfo -> }
        purchases.purchaseProductWith(
            activity,
            storeProduct,
            upgradeInfo,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction?, _: CustomerInfo -> },
        )
        purchases.purchaseProductWith(
            activity,
            storeProduct,
            upgradeInfo,
        ) { _: StoreTransaction?, _: CustomerInfo -> }

        purchases.purchasePackageWith(
            activity,
            packageToPurchase,
            upgradeInfo,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction?, _: CustomerInfo -> },
        )
        purchases.purchasePackageWith(
            activity,
            packageToPurchase,
            upgradeInfo,
        ) { _: StoreTransaction?, _: CustomerInfo -> }

        purchases.purchasePackageWith(
            activity,
            packageToPurchase,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction, _: CustomerInfo -> },
        )

        purchases.purchasePackageWith(
            activity,
            packageToPurchase,
        ) { _: StoreTransaction, _: CustomerInfo -> }

        purchases.allowSharingPlayStoreAccount = true
        purchases.getSubscriptionSkusWith(
            ArrayList<String>(),
            onError = { _: PurchasesError -> },
            onReceiveSkus = { _: List<StoreProduct> -> },
        )
        purchases.getNonSubscriptionSkusWith(
            ArrayList<String>(),
            onError = { _: PurchasesError -> },
            onReceiveSkus = { _: List<StoreProduct> -> },
        )

        Purchases.debugLogsEnabled = false
        val debugLogs: Boolean = Purchases.debugLogsEnabled
    }
}
