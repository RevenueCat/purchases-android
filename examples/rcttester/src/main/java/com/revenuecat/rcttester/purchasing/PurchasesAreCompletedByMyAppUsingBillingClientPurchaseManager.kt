package com.revenuecat.rcttester.purchasing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitSyncPurchases
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.googleProduct
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Purchase manager for purchasesAreCompletedBy MY_APP using BillingClient directly.
 *
 * In this mode:
 * - `purchasesAreCompletedBy` is set to `MY_APP`
 * - `purchaseLogic` is set to `USING_BILLING_CLIENT_DIRECTLY`
 * - The app uses its own BillingClient to launch the purchase flow
 * - After purchase, the app acknowledges/consumes and syncs with RevenueCat
 */
class PurchasesAreCompletedByMyAppUsingBillingClientPurchaseManager(
    private val billingClient: BillingClient,
) : PurchaseManager {

    private var purchaseContinuation: CancellableContinuation<PurchaseUpdateResult>? = null

    private val acknowledgeHelper = BillingClientAcknowledgeHelper(billingClient)

    override val purchaseLogic: PurchaseLogic = object : PurchaseLogic {
        override suspend fun performPurchase(
            activity: Activity,
            rcPackage: Package,
        ): PurchaseLogicResult {
            val result = purchase(activity, rcPackage)
            return when (result) {
                is PurchaseOperationResult.Success,
                is PurchaseOperationResult.SuccessCustomImplementation,
                -> PurchaseLogicResult.Success
                is PurchaseOperationResult.UserCancelled -> PurchaseLogicResult.Cancellation
                is PurchaseOperationResult.Pending -> PurchaseLogicResult.Error()
                is PurchaseOperationResult.Failure -> PurchaseLogicResult.Error()
            }
        }

        override suspend fun performRestore(customerInfo: CustomerInfo): PurchaseLogicResult {
            return try {
                Purchases.sharedInstance.awaitSyncPurchases()
                PurchaseLogicResult.Success
            } catch (e: PurchasesException) {
                Log.e(TAG, "Failed to sync purchases", e)
                PurchaseLogicResult.Error(e.error)
            }
        }
    }

    override suspend fun purchase(
        activity: Activity,
        rcPackage: Package,
    ): PurchaseOperationResult {
        return launchBillingFlow(activity, rcPackage.product)
    }

    override suspend fun purchaseProduct(
        activity: Activity,
        storeProduct: StoreProduct,
    ): PurchaseOperationResult {
        return launchBillingFlow(activity, storeProduct)
    }

    @Suppress("ReturnCount")
    private suspend fun launchBillingFlow(
        activity: Activity,
        storeProduct: StoreProduct,
    ): PurchaseOperationResult {
        if (!ensureConnected()) {
            return PurchaseOperationResult.Failure("Failed to connect to BillingClient")
        }

        val googleProduct = storeProduct.googleProduct
            ?: return PurchaseOperationResult.Failure("Product is not a Google product")

        if (purchaseContinuation != null) {
            return PurchaseOperationResult.Failure("Purchase already in progress")
        }

        val productDetails = googleProduct.productDetails

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .also { params ->
                val subscriptionOption = googleProduct.defaultOption as? GoogleSubscriptionOption
                subscriptionOption?.offerToken?.let { params.setOfferToken(it) }
            }
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val updateResult = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                purchaseContinuation = continuation
                continuation.invokeOnCancellation {
                    purchaseContinuation = null
                }
                val launchResult = billingClient.launchBillingFlow(activity, flowParams)
                if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    purchaseContinuation = null
                    continuation.resume(PurchaseUpdateResult(launchResult, null))
                }
            }
        }

        return handlePurchaseResult(updateResult, storeProduct.type)
    }

    fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        val continuation = purchaseContinuation
        purchaseContinuation = null
        if (continuation != null) {
            continuation.resume(PurchaseUpdateResult(billingResult, purchases))
        } else {
            Log.w(TAG, "Received onPurchasesUpdated but no continuation was set")
        }
    }

    private suspend fun handlePurchaseResult(
        result: PurchaseUpdateResult,
        productType: ProductType,
    ): PurchaseOperationResult {
        val responseCode = result.billingResult.responseCode
        val debugMessage = result.billingResult.debugMessage

        return when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = result.purchases?.firstOrNull()
                if (purchase == null) {
                    Log.e(TAG, "Purchase OK but no purchases returned")
                    return PurchaseOperationResult.Failure("Purchase OK but no purchases returned")
                }
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> handleSuccessfulPurchase(purchase, productType)
                    Purchase.PurchaseState.PENDING -> PurchaseOperationResult.Pending
                    else -> {
                        Log.w(TAG, "Unexpected purchase state: ${purchase.purchaseState}")
                        PurchaseOperationResult.Failure(
                            "Unexpected purchase state: ${purchase.purchaseState}",
                        )
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                PurchaseOperationResult.UserCancelled
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                handleAlreadyOwned()
            }
            else -> {
                Log.e(TAG, "Purchase failed: $responseCode - $debugMessage")
                PurchaseOperationResult.Failure("Purchase failed: $responseCode - $debugMessage")
            }
        }
    }

    private suspend fun handleSuccessfulPurchase(
        purchase: Purchase,
        productType: ProductType,
    ): PurchaseOperationResult {
        acknowledgeHelper.finishTransaction(
            purchaseToken = purchase.purchaseToken,
            productType = productType,
        )
        return PurchaseOperationResult.SuccessCustomImplementation
    }

    private fun handleAlreadyOwned(): PurchaseOperationResult {
        return PurchaseOperationResult.SuccessCustomImplementation
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return suspendCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    continuation.resume(
                        billingResult.responseCode == BillingClient.BillingResponseCode.OK,
                    )
                }

                override fun onBillingServiceDisconnected() {
                    // Will retry connection on next call
                }
            })
        }
    }

    private data class PurchaseUpdateResult(
        val billingResult: BillingResult,
        val purchases: List<Purchase>?,
    )

    companion object {
        private const val TAG = "MyAppBillingClient"
    }
}
