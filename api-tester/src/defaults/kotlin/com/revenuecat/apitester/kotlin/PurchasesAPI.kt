package com.revenuecat.apitester.kotlin

import android.content.Context
import android.content.Intent
import com.revenuecat.purchases.AmazonLWAConsentStatus
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.WebPurchaseRedemption
import com.revenuecat.purchases.amazon.AmazonConfiguration
import com.revenuecat.purchases.awaitCustomerCenterConfigData
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitLogOut
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.awaitSyncAttributesAndOfferingsIfNeeded
import com.revenuecat.purchases.awaitSyncPurchases
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.data.LogInResult
import com.revenuecat.purchases.getAmazonLWAConsentStatus
import com.revenuecat.purchases.getAmazonLWAConsentStatusWith
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.interfaces.GetAmazonLWAConsentStatusCallback
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener
import com.revenuecat.purchases.interfaces.SyncAttributesAndOfferingsCallback
import com.revenuecat.purchases.interfaces.SyncPurchasesCallback
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.models.BillingFeature
import com.revenuecat.purchases.syncAttributesAndOfferingsIfNeededWith
import com.revenuecat.purchases.syncPurchasesWith
import java.util.concurrent.ExecutorService

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock", "DEPRECATION")
private class PurchasesAPI {
    @SuppressWarnings("LongParameterList")
    fun check(
        purchases: Purchases,
        webPurchaseRedemption: WebPurchaseRedemption,
        redeemWebPurchaseListener: RedeemWebPurchaseListener,
        intent: Intent,
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
        val syncAttributesAndOfferingsCallback = object : SyncAttributesAndOfferingsCallback {
            override fun onSuccess(offerings: Offerings) {}
            override fun onError(error: PurchasesError) {}
        }
        val getAmazonLWAConsentStatusCallback = object : GetAmazonLWAConsentStatusCallback {
            override fun onSuccess(status: AmazonLWAConsentStatus) {}
            override fun onError(error: PurchasesError) {}
        }

        purchases.syncAttributesAndOfferingsIfNeeded(syncAttributesAndOfferingsCallback)
        purchases.getAmazonLWAConsentStatus(getAmazonLWAConsentStatusCallback)

        purchases.syncPurchases()
        purchases.syncPurchases(syncPurchasesCallback)

        purchases.logIn("", logInCallback)
        purchases.logOut()
        purchases.logOut(receiveCustomerInfoCallback)

        purchases.invalidateCustomerInfoCache()

        purchases.getCustomerInfo(receiveCustomerInfoCallback)
        purchases.getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, receiveCustomerInfoCallback)

        val purchasesAreCompletedBy: PurchasesAreCompletedBy = purchases.purchasesAreCompletedBy
        purchases.purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT

        val anonymous: Boolean = purchases.isAnonymous

        purchases.onAppBackgrounded()
        purchases.onAppForegrounded()

        val store: Store = purchases.store

        val countryCode = purchases.storefrontCountryCode

        val configuration: PurchasesConfiguration = purchases.currentConfiguration

        purchases.redeemWebPurchase(webPurchaseRedemption, redeemWebPurchaseListener)
        val parsedWebPurchaseRedemption: WebPurchaseRedemption? = Purchases.parseAsWebPurchaseRedemption(intent)
        val parsedWebPurchaseRedemption2: WebPurchaseRedemption? = Purchases.parseAsWebPurchaseRedemption("")
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
        purchases.syncPurchasesWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: CustomerInfo -> },
        )
        purchases.syncPurchasesWith(
            onSuccess = { _: CustomerInfo -> },
        )
        purchases.syncAttributesAndOfferingsIfNeededWith(
            onSuccess = { _: Offerings -> },
        )
        purchases.syncAttributesAndOfferingsIfNeededWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: Offerings -> },
        )
        purchases.getAmazonLWAConsentStatusWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: AmazonLWAConsentStatus -> },
        )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    suspend fun checkCoroutines(
        purchases: Purchases,
    ) {
        val customerInfo: CustomerInfo = purchases.awaitCustomerInfo()
        val customerInfoFetchPolicy: CustomerInfo =
            purchases.awaitCustomerInfo(fetchPolicy = CacheFetchPolicy.FETCH_CURRENT)
        val logInResult: LogInResult = purchases.awaitLogIn("appUserID")
        val (customerInfo2: CustomerInfo, created: Boolean) = purchases.awaitLogIn("appUserID")
        val customerInfo3: CustomerInfo = purchases.awaitLogOut()
        val customerInfo4: CustomerInfo = purchases.awaitRestore()
        val customerInfo5: CustomerInfo = purchases.awaitSyncPurchases()
        var offerings: Offerings = purchases.awaitSyncAttributesAndOfferingsIfNeeded()
        var consentStatus: AmazonLWAConsentStatus = purchases.getAmazonLWAConsentStatus()
        var customerCenterConfigData: CustomerCenterConfigData = purchases.awaitCustomerCenterConfigData()
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
            setOnesignalUserID("")
            setAirshipChannelID("")
            setMixpanelDistinctID("")
            setFirebaseAppInstanceID("")
            setTenjinAnalyticsInstallationID("")
            setMediaSource("")
            setCampaign("")
            setCleverTapID("")
            setKochavaDeviceID("")
            setAdGroup("")
            setAd("")
            setKeyword("")
            setCreative("")
        }
    }

    @Suppress("ForbiddenComment")
    fun checkConfiguration(
        purchasesConfiguration: PurchasesConfiguration,
    ) {
        val features: List<BillingFeature> = ArrayList()
        val configured: Boolean = Purchases.isConfigured

        Purchases.configure(purchasesConfiguration)

        Purchases.debugLogsEnabled = true
    }

    fun checkLogInResult(
        logInResult: LogInResult,
    ) {
        val created: Boolean = logInResult.created
        val customerInfo: CustomerInfo = logInResult.customerInfo
        LogInResult(customerInfo, created)
    }

    fun checkAmazonConfiguration(
        context: Context,
        executorService: ExecutorService,
        purchaseCompleter: PurchasesAreCompletedBy,
    ) {
        val amazonConfiguration: PurchasesConfiguration = AmazonConfiguration.Builder(context, "")
            .appUserID("")
            .observerMode(true)
            .observerMode(false)
            .purchasesAreCompletedBy(purchaseCompleter)
            .showInAppMessagesAutomatically(true)
            .service(executorService)
            .diagnosticsEnabled(true)
            .entitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)
            .build()
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun checkAmazonConfigurationDiagnostics(context: Context, executorService: ExecutorService) {
        val builder: PurchasesConfiguration.Builder = AmazonConfiguration.Builder(context, "")
            .informationalVerificationModeAndDiagnosticsEnabled(true)
    }
}
