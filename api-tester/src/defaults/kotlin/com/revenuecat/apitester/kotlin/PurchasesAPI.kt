package com.revenuecat.apitester.kotlin

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.SyncPurchasesCallback
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.models.BillingFeature
import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.restorePurchasesWith
import com.revenuecat.purchases.syncPurchasesWith
import java.util.concurrent.ExecutorService

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock", "DEPRECATION")
private class PurchasesAPI {
    @SuppressWarnings("LongParameterList")
    fun check(
        purchases: Purchases,
    ) {
        val receiveCustomerInfoCallback = object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError) {}
        }
        val logInCallback = object : LogInCallback {
            override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {}
            override fun onError(error: PurchasesError) {}
        }
        val syncPurchasesCallback = object : SyncPurchasesCallback {
            override fun onSuccess(customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError) {}
        }

        purchases.syncPurchases()
        purchases.syncPurchases(syncPurchasesCallback)

        purchases.logIn("", logInCallback)
        purchases.logOut()
        purchases.logOut(receiveCustomerInfoCallback)

        purchases.restorePurchases(receiveCustomerInfoCallback)
        purchases.invalidateCustomerInfoCache()

        purchases.getCustomerInfo(receiveCustomerInfoCallback)
        purchases.getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, receiveCustomerInfoCallback)

        val finishTransactions: Boolean = purchases.finishTransactions
        purchases.finishTransactions = true

        val anonymous: Boolean = purchases.isAnonymous

        purchases.onAppBackgrounded()
        purchases.onAppForegrounded()

        val store: Store = purchases.store
    }

    @SuppressWarnings("LongParameterList", "EmptyFunctionBlock")
    fun checkPurchasing(
        purchases: Purchases,
        activity: Activity,
        storeProduct: StoreProduct,
        packageToPurchase: Package,
        subscriptionOption: SubscriptionOption,
    ) {
        val purchaseCallback = object : PurchaseCallback {
            override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {}
            override fun onError(error: PurchasesError, userCancelled: Boolean) {}
        }

        val oldProductId = "old"
        val prorationMode = GoogleProrationMode.IMMEDIATE_WITH_TIME_PRORATION
        val isPersonalizedPrice = true

        val purchasePackageBuilder: PurchaseParams.Builder = PurchaseParams.Builder(activity, packageToPurchase)
        purchasePackageBuilder.oldProductId(oldProductId).googleProrationMode(prorationMode)
        val purchasePackageParams: PurchaseParams = purchasePackageBuilder.build()
        purchases.purchase(purchasePackageParams, purchaseCallback)

        val purchaseProductBuilder: PurchaseParams.Builder = PurchaseParams.Builder(activity, storeProduct)
        purchaseProductBuilder.oldProductId(oldProductId).googleProrationMode(prorationMode)
        val purchaseProductParams: PurchaseParams = purchaseProductBuilder.build()
        purchases.purchase(purchaseProductParams, purchaseCallback)

        val purchaseOptionBuilder: PurchaseParams.Builder = PurchaseParams.Builder(activity, subscriptionOption)
        purchaseOptionBuilder.oldProductId(oldProductId).googleProrationMode(prorationMode)
        val purchaseOptionsParams: PurchaseParams = purchaseOptionBuilder.build()
        purchases.purchase(purchaseOptionsParams, purchaseCallback)
    }

    @Suppress("LongMethod", "LongParameterList")
    fun checkListenerConversions(
        purchases: Purchases,
    ) {
        purchases.logInWith(
            "",
            onError = { _: PurchasesError -> },
            onSuccess = { _: CustomerInfo, _: Boolean -> },
        )
        purchases.logOutWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: CustomerInfo -> },
        )
        purchases.getCustomerInfoWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: CustomerInfo -> },
        )
        purchases.getCustomerInfoWith(
            fetchPolicy = CacheFetchPolicy.CACHED_OR_FETCHED,
            onError = { _: PurchasesError -> },
            onSuccess = { _: CustomerInfo -> },
        )
        purchases.restorePurchasesWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: CustomerInfo -> },
        )
        purchases.syncPurchasesWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: CustomerInfo -> },
        )
        purchases.syncPurchasesWith(
            onSuccess = { _: CustomerInfo -> },
        )
    }

    suspend fun checkCoroutines(
        purchases: Purchases,
    ) {
        val customerInfo: CustomerInfo = purchases.awaitCustomerInfo()
        val customerInfoFetchPolicy: CustomerInfo =
            purchases.awaitCustomerInfo(fetchPolicy = CacheFetchPolicy.FETCH_CURRENT)
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

    @Suppress("ForbiddenComment")
    fun checkConfiguration(context: Context, executorService: ExecutorService) {
        val features: List<BillingFeature> = ArrayList()
        val configured: Boolean = Purchases.isConfigured

        val build = PurchasesConfiguration.Builder(context, apiKey = "")
            .appUserID("")
            .observerMode(true)
            .observerMode(false)
            .service(executorService)
            .diagnosticsEnabled(true)
            .entitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)
            .informationalVerificationModeAndDiagnosticsEnabled(true)
            .build()

        Purchases.configure(build)

        Purchases.debugLogsEnabled = true
    }
}
