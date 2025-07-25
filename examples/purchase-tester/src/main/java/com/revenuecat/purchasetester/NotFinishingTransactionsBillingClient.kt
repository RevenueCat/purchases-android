package com.revenuecat.purchasetester

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.revenuecat.purchases.Purchases

object NotFinishingTransactionsBillingClient : PurchasesUpdatedListener, BillingClientStateListener {
    private lateinit var billingClient: BillingClient
    private lateinit var testerLogHandler: TesterLogHandler

    fun start(context: Context, testerLogHandler: TesterLogHandler) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        billingClient.startConnection(this)

        this.testerLogHandler = testerLogHandler
    }

    fun purchase(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String?,
        isOfferPersonalized: Boolean = false,
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
                        .build(),
                ),
            )
            .setIsOfferPersonalized(isOfferPersonalized)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        testerLogHandler.d("[NotFinishingTransactionsBillingClient]", "Called onBillingSetupFinished")
    }
    override fun onBillingServiceDisconnected() {
        testerLogHandler.d("[NotFinishingTransactionsBillingClient]", "Called onBillingServiceDisconnected")
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        testerLogHandler.d(
            "[NotFinishingTransactionsBillingClient]",
            "Called onPurchasesUpdated with status ${billingResult.responseCode}",
        )
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Purchases.sharedInstance.syncPurchases()
        }
    }
}
