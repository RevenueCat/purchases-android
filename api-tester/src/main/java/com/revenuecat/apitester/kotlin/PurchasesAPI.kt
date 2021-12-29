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
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.UpgradeInfo
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getNonSubscriptionSkusWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.getSubscriptionSkusWith
import com.revenuecat.purchases.interfaces.GetSkusResponseListener
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.MakePurchaseListener
import com.revenuecat.purchases.interfaces.ProductChangeListener
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoListener
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.purchasePackageWith
import com.revenuecat.purchases.purchaseProductWith
import com.revenuecat.purchases.restorePurchasesWith
import java.net.URL
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

    @Suppress("RedundantLambdaArrow")
    fun checkListenerConversions(
        purchases: Purchases,
        activity: Activity,
        packageToPurchase: Package,
        skuDetails: SkuDetails,
        upgradeInfo: UpgradeInfo
    ) {
        purchases.getOfferingsWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: Offerings -> }
        )
        purchases.purchaseProductWith(
            activity,
            skuDetails,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: Purchase, _: CustomerInfo -> }
        )
        purchases.purchaseProductWith(
            activity,
            skuDetails,
            upgradeInfo,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: Purchase?, _: CustomerInfo -> }
        )
        purchases.purchasePackageWith(
            activity,
            packageToPurchase,
            upgradeInfo,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: Purchase?, _: CustomerInfo -> }
        )
        purchases.purchasePackageWith(
            activity,
            packageToPurchase,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: Purchase?, _: CustomerInfo -> }
        )
        purchases.restorePurchasesWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: CustomerInfo -> }
        )
        purchases.logInWith(
            "",
            onError = { _: PurchasesError -> },
            onSuccess = { _: CustomerInfo, _: Boolean -> }
        )
        purchases.logOutWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: CustomerInfo -> }
        )
        purchases.getCustomerInfoWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: CustomerInfo -> }
        )
        purchases.getSubscriptionSkusWith(
            ArrayList<String>(),
            onError = { _: PurchasesError -> },
            onReceiveSkus = { _: List<SkuDetails> -> }
        )
        purchases.getNonSubscriptionSkusWith(
            ArrayList<String>(),
            onError = { _: PurchasesError -> },
            onReceiveSkus = { _: List<SkuDetails> -> }
        )
    }

    fun check(purchases: Purchases, attributes: Map<String, String>) {
        with(purchases) {
            setAttributes(attributes)
            setEmail("")
            setPhoneNumber("")
            setDisplayName("")
            setPushToken("")
            collectDeviceIdentifiers()
            setAdjustID("")
            setAppsflyerID("")
            setFBAnonymousID("")
            setMparticleID("")
            setOnesignalID("")
            setAirshipChannelID("")
            setMediaSource("")
            setCampaign("")
            setAdGroup("")
            setAd("")
            setKeyword("")
            setCreative("")
        }
    }

    @Suppress("RemoveRedundantQualifierName", "RedundantLambdaArrow", "ForbiddenComment")
    fun checkConfiguration(context: Context, executorService: ExecutorService) {
        val features: List<BillingFeature> = ArrayList()
        val configured: Boolean = Purchases.isConfigured

        Purchases.configure(context, "")
        Purchases.configure(context, "", "")
        Purchases.configure(context, "", "", true)
        Purchases.configure(context, "", "", false, executorService)

        Purchases.canMakePayments(context, features) { _: Boolean -> }
        Purchases.canMakePayments(context) { _: Boolean -> }

        Purchases.debugLogsEnabled = false
        val debugLogs: Boolean = Purchases.debugLogsEnabled

        Purchases.proxyURL = URL("")
        val url: URL? = Purchases.proxyURL

        val instance: Purchases = Purchases.sharedInstance

        // TODO: add the builder version once amazon is merged
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
        }.exhaustive
    }
}
