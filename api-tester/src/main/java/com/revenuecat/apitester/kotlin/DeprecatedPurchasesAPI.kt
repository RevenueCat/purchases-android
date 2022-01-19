package com.revenuecat.apitester.kotlin

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.UpgradeInfo
import com.revenuecat.purchases.interfaces.GetSkusResponseListener
import com.revenuecat.purchases.interfaces.MakePurchaseListener
import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.ProductChangeListener
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.purchaseProductWith
import java.util.ArrayList
import java.util.concurrent.ExecutorService

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock", "LongParameterList")
private class DeprecatedPurchasesAPI {
    fun check(
        purchases: Purchases,
        activity: Activity,
        skuDetails: SkuDetails,
        storeProduct: StoreProduct,
        packageToPurchase: Package,
        upgradeInfo: UpgradeInfo
    ) {
        val skus = ArrayList<String>()
        val receiveOfferingsListener = object : ReceiveOfferingsListener {
            override fun onReceived(offerings: Offerings) {}
            override fun onError(error: PurchasesError) {}
        }
        val skusResponseListener = object : GetSkusResponseListener {
            override fun onReceived(skus: List<SkuDetails>) {}
            override fun onError(error: PurchasesError) {}
        }
        val productChangeListener = object : ProductChangeListener {
            override fun onCompleted(purchase: Purchase?, purchaserInfo: PurchaserInfo) {}
            override fun onError(error: PurchasesError, userCancelled: Boolean) {}
        }
        val makePurchaseListener = object : MakePurchaseListener {
            override fun onCompleted(purchase: Purchase, purchaserInfo: PurchaserInfo) {}
            override fun onError(error: PurchasesError, userCancelled: Boolean) {}
        }
        val receiveCustomerInfoListener = object : ReceivePurchaserInfoListener {
            override fun onReceived(purchaserInfo: PurchaserInfo) {}
            override fun onError(error: PurchasesError) {}
        }
        purchases.getOfferings(receiveOfferingsListener)
        purchases.getSubscriptionSkus(skus, skusResponseListener)
        purchases.getNonSubscriptionSkus(skus, skusResponseListener)

        val productChangeCallback = object : ProductChangeCallback {
            override fun onCompleted(storeTransaction: StoreTransaction?, customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError, userCancelled: Boolean) {}
        }
        val purchaseCallback = object : PurchaseCallback {
            override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError, userCancelled: Boolean) {}
        }
        purchases.purchaseProduct(activity, skuDetails, upgradeInfo, productChangeListener)
        purchases.purchaseProduct(activity, storeProduct, upgradeInfo, productChangeListener)
        purchases.purchaseProduct(activity, skuDetails, upgradeInfo, productChangeCallback)
        purchases.purchaseProduct(activity, skuDetails, makePurchaseListener)
        purchases.purchaseProduct(activity, storeProduct, makePurchaseListener)
        purchases.purchaseProduct(activity, skuDetails, purchaseCallback)
        purchases.purchasePackage(activity, packageToPurchase, upgradeInfo, productChangeListener)
        purchases.purchasePackage(activity, packageToPurchase, makePurchaseListener)
        purchases.restorePurchases(receiveCustomerInfoListener)
        purchases.logOut(receiveCustomerInfoListener)
        purchases.getPurchaserInfo(receiveCustomerInfoListener)
        purchases.getPurchaserInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError) {}
        })
        purchases.getCustomerInfo(receiveCustomerInfoListener)
        purchases.invalidatePurchaserInfoCache()
        purchases.removeUpdatedPurchaserInfoListener()

        val updatedPurchaserInfoListener: UpdatedPurchaserInfoListener? = purchases.updatedPurchaserInfoListener
        purchases.updatedPurchaserInfoListener = object : UpdatedPurchaserInfoListener {
            override fun onReceived(purchaserInfo: PurchaserInfo) {
            }
        }

        purchases.allowSharingPlayStoreAccount = true
    }

    @Suppress("RedundantLambdaArrow")
    fun checkListenerConversions(
        purchases: Purchases,
        activity: Activity,
        skuDetails: SkuDetails,
        upgradeInfo: UpgradeInfo
    ) {
        purchases.purchaseProductWith(
            activity,
            skuDetails,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: Purchase, _: PurchaserInfo -> }
        )
        purchases.purchaseProductWith(
            activity,
            skuDetails,
            upgradeInfo,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: Purchase?, _: PurchaserInfo -> }
        )
    }

    @Suppress("RemoveRedundantQualifierName", "RedundantLambdaArrow", "ForbiddenComment")
    fun checkConfiguration(context: Context, executorService: ExecutorService) {
        Purchases.configure(context, "")
        Purchases.configure(context, "", "")
        Purchases.configure(context, "", "", true)
        Purchases.configure(context, "", "", false, executorService)
    }
}
