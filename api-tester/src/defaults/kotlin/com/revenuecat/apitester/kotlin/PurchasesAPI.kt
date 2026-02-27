package com.revenuecat.apitester.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.revenuecat.purchases.AmazonLWAConsentStatus
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.Purchases.Companion.sharedInstance
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.WebPurchaseRedemption
import com.revenuecat.purchases.amazon.AmazonConfiguration
import com.revenuecat.purchases.awaitCustomerCenterConfigData
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitGetVirtualCurrencies
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitLogOut
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.awaitRestoreResult
import com.revenuecat.purchases.awaitStorefrontLocale
import com.revenuecat.purchases.awaitSyncAttributesAndOfferingsIfNeeded
import com.revenuecat.purchases.awaitSyncPurchases
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.customercenter.CustomerCenterManagementOption
import com.revenuecat.purchases.data.LogInResult
import com.revenuecat.purchases.getAmazonLWAConsentStatus
import com.revenuecat.purchases.getAmazonLWAConsentStatusWith
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getStorefrontLocaleWith
import com.revenuecat.purchases.getVirtualCurrenciesWith
import com.revenuecat.purchases.interfaces.GetAmazonLWAConsentStatusCallback
import com.revenuecat.purchases.interfaces.GetStorefrontLocaleCallback
import com.revenuecat.purchases.interfaces.GetVirtualCurrenciesCallback
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
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import java.util.Locale
import java.util.concurrent.ExecutorService

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock", "DEPRECATION", "TooManyFunctions")
private class PurchasesAPI {
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
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

        val getVirtualCurrenciesCallback = object : GetVirtualCurrenciesCallback {
            override fun onReceived(virtualCurrencies: VirtualCurrencies) {}
            override fun onError(error: PurchasesError) {}
        }

        val getStorefrontLocaleCallback = object : GetStorefrontLocaleCallback {
            override fun onReceived(storefrontLocale: Locale) {}
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

        val configuration: PurchasesConfiguration = purchases.currentConfiguration

        purchases.redeemWebPurchase(webPurchaseRedemption, redeemWebPurchaseListener)
        val parsedWebPurchaseRedemption: WebPurchaseRedemption? = Purchases.parseAsWebPurchaseRedemption(intent)
        val parsedWebPurchaseRedemption2: WebPurchaseRedemption? = Purchases.parseAsWebPurchaseRedemption("")

        purchases.getVirtualCurrencies(callback = getVirtualCurrenciesCallback)
        purchases.invalidateVirtualCurrenciesCache()
        val cachedVirtualCurrencies: VirtualCurrencies? = purchases.cachedVirtualCurrencies

        val locale: Locale? = purchases.storefrontLocale
        purchases.getStorefrontLocale(getStorefrontLocaleCallback)
    }

    @Suppress("LongParameterList")
    fun checkSyncAmazonPurchase(
        purchases: Purchases,
        productId: String,
        receiptId: String,
        amazonUserId: String,
        isoCurrencyCode: String?,
        price: Double?,
        purchaseTime: Long,
    ) {
        purchases.syncAmazonPurchase(productId, receiptId, amazonUserId, isoCurrencyCode, price, purchaseTime)
        purchases.syncAmazonPurchase(productId, receiptId, amazonUserId, isoCurrencyCode, price)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
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
        purchases.getVirtualCurrenciesWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: VirtualCurrencies -> },
        )
        purchases.getStorefrontLocaleWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: Locale -> },
        )
    }

    @OptIn(InternalRevenueCatAPI::class, ExperimentalPreviewRevenueCatPurchasesAPI::class)
    suspend fun checkCoroutines(
        purchases: Purchases,
    ) {
        val customerInfo: CustomerInfo = purchases.awaitCustomerInfo()
        val customerInfoFetchPolicy: CustomerInfo =
            purchases.awaitCustomerInfo(fetchPolicy = CacheFetchPolicy.FETCH_CURRENT)
        val logInResult: LogInResult = purchases.awaitLogIn("appUserID")
        val customerInfo3: CustomerInfo = purchases.awaitLogOut()
        val customerInfo4: CustomerInfo = purchases.awaitRestore()
        val customerInfo5: CustomerInfo = purchases.awaitSyncPurchases()
        val customerInfo6: Result<CustomerInfo> = purchases.awaitRestoreResult()
        var offerings: Offerings = purchases.awaitSyncAttributesAndOfferingsIfNeeded()
        var consentStatus: AmazonLWAConsentStatus = purchases.getAmazonLWAConsentStatus()
        var customerCenterConfigData: CustomerCenterConfigData = purchases.awaitCustomerCenterConfigData()
        val getVirtualCurrenciesResult: VirtualCurrencies = purchases.awaitGetVirtualCurrencies()
        val storefrontLocale: Locale = purchases.awaitStorefrontLocale()
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
            setPostHogUserId("")
            setMediaSource("")
            setCampaign("")
            setCleverTapID("")
            setKochavaDeviceID("")
            setAirbridgeDeviceID("")
            setSolarEngineDistinctId("")
            setSolarEngineAccountId("")
            setSolarEngineVisitorId("")
            setAdGroup("")
            setAd("")
            setKeyword("")
            setCreative("")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun checkSetAppsFlyerConversionData(purchases: Purchases) {
        val mapStringAny: Map<String, Any> = emptyMap()
        purchases.setAppsFlyerConversionData(mapStringAny)

        val mapNullableKeyAny: Map<String?, Any> = emptyMap()
        purchases.setAppsFlyerConversionData(mapNullableKeyAny)

        val mapNullableKeyNullableValue: Map<String?, Any?> = emptyMap()
        purchases.setAppsFlyerConversionData(mapNullableKeyNullableValue)

        val nullableMapNullableKeyNullableValue: Map<String?, Any?>? = null
        purchases.setAppsFlyerConversionData(nullableMapNullableKeyNullableValue)

        val nullableMapStringAny: Map<String, Any>? = emptyMap()
        purchases.setAppsFlyerConversionData(nullableMapStringAny)

        val mapStringString: Map<String, String> = emptyMap()
        purchases.setAppsFlyerConversionData(mapStringString)

        val mapStringNullableString: Map<String, String?> = emptyMap()
        purchases.setAppsFlyerConversionData(mapStringNullableString)

        val mapNullableKeyNullableString: Map<String?, String?> = emptyMap()
        purchases.setAppsFlyerConversionData(mapNullableKeyNullableString)

        val mapStringInt: Map<String, Int> = emptyMap()
        purchases.setAppsFlyerConversionData(mapStringInt)

        val mapStringNullableInt: Map<String, Int?> = emptyMap()
        purchases.setAppsFlyerConversionData(mapStringNullableInt)

        val mapNullableKeyNullableInt: Map<String?, Int?> = emptyMap()
        purchases.setAppsFlyerConversionData(mapNullableKeyNullableInt)
    }

    @Suppress("UNUSED_PARAMETER")
    fun checkSetAppstackAttributionParams(purchases: Purchases) {
        val mapStringAny: Map<String, Any> = emptyMap()
        purchases.setAppstackAttributionParams(mapStringAny)

        val mapNullableKeyAny: Map<String?, Any> = emptyMap()
        purchases.setAppstackAttributionParams(mapNullableKeyAny)

        val mapNullableKeyNullableValue: Map<String?, Any?> = emptyMap()
        purchases.setAppstackAttributionParams(mapNullableKeyNullableValue)

        val nullableMapNullableKeyNullableValue: Map<String?, Any?>? = null
        purchases.setAppstackAttributionParams(nullableMapNullableKeyNullableValue)

        val nullableMapStringAny: Map<String, Any>? = emptyMap()
        purchases.setAppstackAttributionParams(nullableMapStringAny)

        val mapStringString: Map<String, String> = emptyMap()
        purchases.setAppstackAttributionParams(mapStringString)

        val mapStringNullableString: Map<String, String?> = emptyMap()
        purchases.setAppstackAttributionParams(mapStringNullableString)

        val mapNullableKeyNullableString: Map<String?, String?> = emptyMap()
        purchases.setAppstackAttributionParams(mapNullableKeyNullableString)

        val mapStringInt: Map<String, Int> = emptyMap()
        purchases.setAppstackAttributionParams(mapStringInt)

        val mapStringNullableInt: Map<String, Int?> = emptyMap()
        purchases.setAppstackAttributionParams(mapStringNullableInt)

        val mapNullableKeyNullableInt: Map<String?, Int?> = emptyMap()
        purchases.setAppstackAttributionParams(mapNullableKeyNullableInt)
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

    fun checkCustomerCenter() {
        val customerInfoListener: CustomerCenterListener = object : CustomerCenterListener {
            override fun onRestoreStarted() {
            }
        }
        val customerInfoListener2: CustomerCenterListener = object : CustomerCenterListener {
            override fun onFeedbackSurveyCompleted(feedbackSurveyOptionId: String) {
            }

            override fun onShowingManageSubscriptions() {
            }

            override fun onRestoreCompleted(customerInfo: CustomerInfo) {
            }

            override fun onRestoreFailed(error: PurchasesError) {
            }

            override fun onRestoreStarted() {
            }

            override fun onManagementOptionSelected(action: CustomerCenterManagementOption) {
                when (action) {
                    CustomerCenterManagementOption.MissingPurchase -> {
                    }
                    CustomerCenterManagementOption.Cancel -> {
                    }
                    is CustomerCenterManagementOption.CustomUrl -> {
                        val uri: Uri = action.uri
                    }
                }
            }
        }
        sharedInstance.customerCenterListener = object : CustomerCenterListener {}
        sharedInstance.customerCenterListener = customerInfoListener
    }
}
