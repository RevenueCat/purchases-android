package com.revenuecat.purchasetester

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.revenuecat.purchases.Purchases

object ObserverModeBillingClient : PurchasesUpdatedListener, BillingClientStateListener {
    private lateinit var billingClient: BillingClient

    fun start(context: Context) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        billingClient.startConnection(this)
    }

    fun purchase(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String?,
        isOfferPersonalized: Boolean = false
    ) {
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .also { params ->
                            offerToken?.let {
                                params.setOfferToken(it)
                            }
                        }
                        .build()
                )
            )
            .setIsOfferPersonalized(isOfferPersonalized)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        print("onBillingSetupFinished")
    }
    override fun onBillingServiceDisconnected() {
        print("onBillingServiceDisconnected")
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Purchases.sharedInstance.syncPurchases()
        }
    }
}
