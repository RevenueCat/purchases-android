package com.revenuecat.apitester.kotlin

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.BillingFeature
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.Purchases.Companion.canMakePayments
import com.revenuecat.purchases.Purchases.Companion.configure
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.UpgradeInfo
import com.revenuecat.purchases.interfaces.GetSkusResponseListener
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.MakePurchaseListener
import com.revenuecat.purchases.interfaces.ProductChangeListener
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoListener
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import java.util.ArrayList
import java.util.concurrent.ExecutorService

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock")
private class PurchasesAPI {
    fun check(
        purchases: Purchases,
        activity: Activity,
        skuDetails: SkuDetails,
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
        val purchaseChangeListener = object : ProductChangeListener {
            override fun onCompleted(purchase: Purchase?, customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError, userCancelled: Boolean) {}
        }
        val makePurchaseListener = object : MakePurchaseListener {
            override fun onCompleted(purchase: Purchase, customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError, userCancelled: Boolean) {}
        }
        val receiveCustomerInfoListener = object : ReceiveCustomerInfoListener {
            override fun onReceived(customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError) {}
        }
        val logInCallback = object : LogInCallback {
            override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {}
            override fun onError(error: PurchasesError) {}
        }
        purchases.syncPurchases()
        purchases.getOfferings(receiveOfferingsListener)
        purchases.getSubscriptionSkus(skus, skusResponseListener)
        purchases.getNonSubscriptionSkus(skus, skusResponseListener)
        purchases.purchaseProduct(activity, skuDetails, upgradeInfo, purchaseChangeListener)
        purchases.purchaseProduct(activity, skuDetails, makePurchaseListener)
        purchases.purchasePackage(activity, packageToPurchase, upgradeInfo, purchaseChangeListener)
        purchases.purchasePackage(activity, packageToPurchase, makePurchaseListener)
        purchases.restorePurchases(receiveCustomerInfoListener)
        purchases.logIn("", logInCallback)
        purchases.logOut()
        purchases.logOut(receiveCustomerInfoListener)
        val appUserID: String = purchases.appUserID

        purchases.getCustomerInfo(receiveCustomerInfoListener)
        purchases.removeUpdatedCustomerInfoListener()
        purchases.invalidateCustomerInfoCache()
        purchases.close()

        val finishTransactions: Boolean = purchases.finishTransactions
        purchases.finishTransactions = true
        val updatedCustomerInfoListener: UpdatedCustomerInfoListener? = purchases.updatedCustomerInfoListener
        purchases.updatedCustomerInfoListener = UpdatedCustomerInfoListener { _: CustomerInfo? -> }

        val anonymous: Boolean = purchases.isAnonymous

        purchases.onAppBackgrounded()
        purchases.onAppForegrounded()
    }

    fun check(purchases: Purchases, attributes: Map<String, String>) {
        purchases.setAttributes(attributes)
        purchases.setEmail("")
        purchases.setPhoneNumber("")
        purchases.setDisplayName("")
        purchases.setPushToken("")
        purchases.collectDeviceIdentifiers()
        purchases.setAdjustID("")
        purchases.setAppsflyerID("")
        purchases.setFBAnonymousID("")
        purchases.setMparticleID("")
        purchases.setOnesignalID("")
        purchases.setAirshipChannelID("")
        purchases.setMediaSource("")
        purchases.setCampaign("")
        purchases.setAdGroup("")
        purchases.setAd("")
        purchases.setKeyword("")
        purchases.setCreative("")
    }

    @Suppress("RemoveRedundantQualifierName", "RedundantLambdaArrow")
    fun checkConfiguration(context: Context, executorService: ExecutorService) {
        val features: List<BillingFeature> = ArrayList()
        val configured: Boolean = Purchases.isConfigured

        configure(context, "")
        configure(context, "", "")
        configure(context, "", "", true)
        configure(context, "", "", false, executorService)

        canMakePayments(context, features) { _: Boolean -> }
        canMakePayments(context) { _: Boolean -> }
    }

    fun check(network: Purchases.AttributionNetwork) {
        when (network) {
            Purchases.AttributionNetwork.ADJUST,
            Purchases.AttributionNetwork.APPSFLYER,
            Purchases.AttributionNetwork.BRANCH,
            Purchases.AttributionNetwork.TENJIN,
            Purchases.AttributionNetwork.FACEBOOK,
            Purchases.AttributionNetwork.MPARTICLE
            -> {}
        }
    }
}
