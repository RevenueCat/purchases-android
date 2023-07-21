package com.revenuecat.apitester.kotlin

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.getProductsWith
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.BillingFeature
import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.purchaseWith
import java.net.URL

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock")
private class PurchasesCommonAPI {
    @SuppressWarnings("LongParameterList")
    fun check(
        purchases: Purchases,
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

        purchases.getOfferings(receiveOfferingsCallback)

        purchases.getProducts(productIds, productsResponseCallback)
        purchases.getProducts(productIds, ProductType.SUBS, productsResponseCallback)

        val appUserID: String = purchases.appUserID

        purchases.removeUpdatedCustomerInfoListener()
        purchases.close()

        val updatedCustomerInfoListener: UpdatedCustomerInfoListener? = purchases.updatedCustomerInfoListener
        purchases.updatedCustomerInfoListener = UpdatedCustomerInfoListener { _: CustomerInfo? -> }
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
        purchaseParams: PurchaseParams,
    ) {
        purchases.getOfferingsWith(
            onError = { _: PurchasesError -> },
            onSuccess = { _: Offerings -> },
        )
        purchases.getProductsWith(
            listOf(""),
            onError = { _: PurchasesError -> },
            onGetStoreProducts = { _: List<StoreProduct> -> },
        )
        purchases.getProductsWith(
            listOf(""),
            ProductType.SUBS,
            onError = { _: PurchasesError -> },
            onGetStoreProducts = { _: List<StoreProduct> -> },
        )
        purchases.purchaseWith(
            purchaseParams,
            onError = { _: PurchasesError, _: Boolean -> },
            onSuccess = { _: StoreTransaction?, _: CustomerInfo -> },
        )
        purchases.purchaseWith(
            purchaseParams,
        ) { _: StoreTransaction?, _: CustomerInfo -> }
    }

    suspend fun checkCoroutines(
        purchases: Purchases,
        activity: Activity,
        packageToPurchase: Package,
    ) {
        val offerings: Offerings = purchases.awaitOfferings()

        val purchasePackageBuilder: PurchaseParams.Builder = PurchaseParams.Builder(activity, packageToPurchase)
        val (transaction, newCustomerInfo) = purchases.awaitPurchase(purchasePackageBuilder.build())
        val purchaseResult: PurchaseResult = purchases.awaitPurchase(purchasePackageBuilder.build())
    }

    @Suppress("ForbiddenComment")
    fun checkConfiguration(context: Context) {
        val features: List<BillingFeature> = ArrayList()
        val configured: Boolean = Purchases.isConfigured

        Purchases.canMakePayments(context, features) { _: Boolean -> }
        Purchases.canMakePayments(context) { _: Boolean -> }

        Purchases.logLevel = LogLevel.INFO
        val logLevel: LogLevel = Purchases.logLevel

        Purchases.proxyURL = URL("")
        val url: URL? = Purchases.proxyURL

        val instance: Purchases = Purchases.sharedInstance
    }

    fun checkLogHandler() {
        Purchases.logHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {}
            override fun e(tag: String, msg: String, throwable: Throwable?) {}
        }
        val handler = Purchases.logHandler
    }

    fun checkLogLevel(level: LogLevel) {
        when (level) {
            LogLevel.VERBOSE,
            LogLevel.DEBUG,
            LogLevel.INFO,
            LogLevel.WARN,
            LogLevel.ERROR,
            -> {}
        }.exhaustive
    }
}
