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
import com.revenuecat.purchases.models.GoogleProductData

object ObserverModeBillingClient : PurchasesUpdatedListener, BillingClientStateListener {
    private lateinit var billingClient: BillingClient
    private lateinit var testerLogHandler: TesterLogHandler

    fun start(context: Context, testerLogHandler: TesterLogHandler) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        billingClient.startConnection(this)

        this.testerLogHandler = testerLogHandler
    }

    fun purchase(
        activity: Activity,
        productData: GoogleProductData,
        offerToken: String?,
        isOfferPersonalized: Boolean = false,
    ) {
        when (productData) {
            is GoogleProductData.Product -> {
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productData.data)
                                .also { params ->
                                    offerToken?.let {
                                        params.setOfferToken(it)
                                    }
                                }
                                .build(),
                        ),
                    )
                    .setIsOfferPersonalized(isOfferPersonalized)
                    .build()
                billingClient.launchBillingFlow(activity, flowParams)
            }
            is GoogleProductData.Sku -> {
                // TODO: Handle
            }
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        testerLogHandler.d("[ObserverModeBillingClient]", "Called onBillingSetupFinished")
    }
    override fun onBillingServiceDisconnected() {
        testerLogHandler.d("[ObserverModeBillingClient]", "Called onBillingServiceDisconnected")
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        testerLogHandler.d(
            "[ObserverModeBillingClient]",
            "Called onPurchasesUpdated with status ${billingResult.responseCode}",
        )
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Purchases.sharedInstance.syncPurchases()
        }
    }
}
