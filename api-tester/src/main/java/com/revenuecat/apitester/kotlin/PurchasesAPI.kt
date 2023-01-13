package com.revenuecat.apitester.kotlin

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.UpgradeInfo
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getNonSubscriptionSkusWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.getSubscriptionSkusWith
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.models.BillingFeature
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.purchasePackageWith
import com.revenuecat.purchases.purchaseProductWith
import com.revenuecat.purchases.purchaseSubscriptionOptionWith
import com.revenuecat.purchases.restorePurchasesWith
import java.net.URL
import java.util.concurrent.ExecutorService

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock", "RemoveExplicitTypeArguments", "RedundantLambdaArrow")
private class PurchasesAPI {
    @SuppressWarnings("LongParameterList")
    fun check(
        purchases: Purchases,
        activity: Activity,
        storeProduct: StoreProduct,
        packageToPurchase: Package,
        purchaseOption: PurchaseOption,
        upgradeInfo: UpgradeInfo
    ) {
        val productIds = ArrayList<String>()
        val receiveOfferingsCallback = object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {}
            override fun onError(error: PurchasesError) {}
        }
        val productsResponseCallback = object : GetStoreProductsCallback {
            override fun onReceived(storeProducts: List<StoreProduct>) {}
            override fun onError(error: PurchasesError) {}
        }
        val purchaseChangeCallback = object : ProductChangeCallback {
            override fun onCompleted(storeTransaction: StoreTransaction?, customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError, userCancelled: Boolean) {}
        }
        val purchaseCallback = object : PurchaseCallback {
            override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError, userCancelled: Boolean) {}
        }
        val receiveCustomerInfoCallback = object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError) {}
        }
        val logInCallback = object : LogInCallback {
            override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {}
            override fun onError(error: PurchasesError) {}
        }
        purchases.syncPurchases()
        purchases.getOfferings(receiveOfferingsCallback)

        purchases.getProducts(productIds, productsResponseCallback)

        // we need these for hybrids... these all fall back on some "best offer" or just purchase the base plan
        purchases.purchaseProduct(activity, storeProduct, upgradeInfo, purchaseChangeCallback)
        purchases.purchaseProduct(activity, storeProduct, purchaseCallback)
        purchases.purchasePackage(activity, packageToPurchase, upgradeInfo, purchaseChangeCallback)
        purchases.purchasePackage(activity, packageToPurchase, purchaseCallback)

        purchases.purchaseSubscriptionOption(
            activity,
            purchaseOption,
            upgradeInfo,
            purchaseChangeCallback
        )
        purchases.purchaseSubscriptionOption(activity, purchaseOption, purchaseCallback)

        purchases.restorePurchases(receiveCustomerInfoCallback)
        purchases.logIn("", logInCallback)
        purchases.logOut()
        purchases.logOut(receiveCustomerInfoCallback)
        val appUserID: String = purchases.appUserID

        purchases.getCustomerInfo(receiveCustomerInfoCallback)
        purchases.getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, receiveCustomerInfoCallback)
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

    @Suppress("RedundantLambdaArrow", "LongMethod", "LongParameterList")
    fun checkListenerConversions(
        purchases: Purchases,
        activity: Activity,
        packageToPurchase: Package,
        storeProduct: StoreProduct,
        purchaseOption: PurchaseOption,
        upgradeInfo: UpgradeInfo
    ) {
        purchases.getOfferingsWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: Offerings -> }
        )
        purchases.purchaseProductWith(
            activity,
            storeProduct,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction, _: CustomerInfo -> }
        )
        purchases.purchaseProductWith(
            activity,
            storeProduct,
            upgradeInfo,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction?, _: CustomerInfo -> }
        )
        purchases.purchasePackageWith(
            activity,
            packageToPurchase,
            upgradeInfo,
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
            purchaseOption,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction, _: CustomerInfo -> }
        )
        purchases.purchaseSubscriptionOptionWith(
            activity,
            purchaseOption,
            upgradeInfo,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction?, _: CustomerInfo -> }
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
        purchases.getCustomerInfoWith(
            fetchPolicy = CacheFetchPolicy.CACHED_OR_FETCHED,
            onError = { _: PurchasesError -> },
            onSuccess = { _: CustomerInfo -> }
        )
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
            setMixpanelDistinctID("")
            setFirebaseAppInstanceID("")
            setMediaSource("")
            setCampaign("")
            setCleverTapID("")
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

        val build = PurchasesConfiguration.Builder(context, apiKey = "")
            .appUserID("")
            .observerMode(true)
            .observerMode(false)
            .service(executorService)
            .build()

        Purchases.configure(build)

        Purchases.canMakePayments(context, features) { _: Boolean -> }
        Purchases.canMakePayments(context) { _: Boolean -> }

        Purchases.debugLogsEnabled = false
        val debugLogs: Boolean = Purchases.debugLogsEnabled

        Purchases.proxyURL = URL("")
        val url: URL? = Purchases.proxyURL

        val instance: Purchases = Purchases.sharedInstance
    }

    fun checkLogHandler() {
        Purchases.logHandler = object : LogHandler {
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {}
            override fun e(tag: String, msg: String, throwable: Throwable?) {}
        }
        val handler = Purchases.logHandler
    }
}
