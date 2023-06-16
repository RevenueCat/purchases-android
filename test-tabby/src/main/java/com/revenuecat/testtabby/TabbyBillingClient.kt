package com.revenuecat.testtabby

import android.app.Activity
import android.content.Context
import androidx.annotation.AnyThread
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.InAppMessageParams
import com.android.billingclient.api.InAppMessageResponseListener
import com.android.billingclient.api.PriceChangeConfirmationListener
import com.android.billingclient.api.PriceChangeFlowParams
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener

class TabbyBillingClient : BillingClient() {

    companion object {

        @JvmStatic
        fun newBuilder(context: Context) = TabbyBillingClientBuilder(context)
    }

    override fun getConnectionState(): Int {
        return BillingClient.BillingResponseCode.OK
    }

    override fun isFeatureSupported(p0: String): BillingResult {
        return BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
    }

    override fun launchBillingFlow(p0: Activity, p1: BillingFlowParams): BillingResult {
        return BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
    }

    override fun showInAppMessages(
        p0: Activity, p1: InAppMessageParams, p2: InAppMessageResponseListener
    ): BillingResult {
        return BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
    }

    override fun acknowledgePurchase(p0: AcknowledgePurchaseParams, p1: AcknowledgePurchaseResponseListener) {
        p1.onAcknowledgePurchaseResponse(
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        )
    }

    override fun consumeAsync(p0: ConsumeParams, p1: ConsumeResponseListener) {
        p1.onConsumeResponse(
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(), ""
        )
    }

    override fun endConnection() {
        // no-op
    }

    override fun launchPriceChangeConfirmationFlow(
        p0: Activity,
        p1: PriceChangeFlowParams,
        p2: PriceChangeConfirmationListener
    ) {
        TODO("Not yet implemented")
    }

    override fun queryProductDetailsAsync(p0: QueryProductDetailsParams, p1: ProductDetailsResponseListener) {
        p1.onProductDetailsResponse(
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(), emptyList()
        )
    }

    override fun queryPurchaseHistoryAsync(p0: QueryPurchaseHistoryParams, p1: PurchaseHistoryResponseListener) {
        p1.onPurchaseHistoryResponse(
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(), emptyList()
        )
    }

    @Deprecated("Deprecated in Java")
    override fun queryPurchaseHistoryAsync(p0: String, p1: PurchaseHistoryResponseListener) {
        p1.onPurchaseHistoryResponse(
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(), emptyList()
        )
    }

    override fun queryPurchasesAsync(p0: QueryPurchasesParams, p1: PurchasesResponseListener) {
        p1.onQueryPurchasesResponse(
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(), emptyList()
        )
    }

    @Deprecated("Deprecated in Java")
    override fun queryPurchasesAsync(p0: String, p1: PurchasesResponseListener) {
        p1.onQueryPurchasesResponse(
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(), emptyList()
        )
    }

    override fun querySkuDetailsAsync(p0: SkuDetailsParams, p1: SkuDetailsResponseListener) {
        p1.onSkuDetailsResponse(
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(), emptyList()
        )
    }

    override fun startConnection(p0: BillingClientStateListener) {
        p0.onBillingSetupFinished(
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        )
    }

    override fun isReady(): Boolean {
        return true
    }

    @AnyThread
    class TabbyBillingClientBuilder(val context: Context) {

        fun enablePendingPurchases(): TabbyBillingClientBuilder {
            return this
        }

        fun setListener(listener: PurchasesUpdatedListener): TabbyBillingClientBuilder {
            return this
        }

        fun build(): BillingClient {
            return TabbyBillingClient()
        }
    }
}