package com.revenuecat.rcttester.purchasing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitSyncPurchases
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult

/**
 * Purchase manager for purchasesAreCompletedBy MY_APP with RevenueCat purchase methods.
 *
 * In this mode:
 * - `purchasesAreCompletedBy` is set to `MY_APP`
 * - `purchaseLogic` is set to `THROUGH_REVENUECAT`
 * - RevenueCat's `purchase()` is called, but transactions are NOT auto-finished
 * - The app must acknowledge/consume purchases itself to prevent Google auto-refunds
 */
class PurchasesAreCompletedByMyAppThroughRevenueCatPurchaseManager(
    billingClient: BillingClient,
) : PurchaseManager {

    private val acknowledgeHelper = BillingClientAcknowledgeHelper(billingClient)

    override val purchaseLogic: PurchaseLogic = object : PurchaseLogic {
        override suspend fun performPurchase(
            activity: Activity,
            rcPackage: Package,
        ): PurchaseLogicResult {
            val result = purchase(activity, rcPackage)
            return when (result) {
                is PurchaseOperationResult.Success -> PurchaseLogicResult.Success
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
                PurchaseLogicResult.Error()
            }
        }
    }

    override suspend fun purchase(activity: Activity, rcPackage: Package): PurchaseOperationResult {
        return try {
            val purchaseParams = PurchaseParams.Builder(activity, rcPackage).build()
            val result = Purchases.sharedInstance.awaitPurchase(purchaseParams)

            // In MY_APP mode, the SDK does NOT acknowledge/consume purchases.
            // We must do it ourselves to prevent Google from auto-refunding after 3 days.
            finishTransaction(
                purchaseToken = result.storeTransaction.purchaseToken,
                productType = result.storeTransaction.type,
            )

            PurchaseOperationResult.Success(result.customerInfo)
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) {
                PurchaseOperationResult.UserCancelled
            } else {
                PurchaseOperationResult.Failure(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun finishTransaction(purchaseToken: String, productType: ProductType) {
        val success = when (productType) {
            ProductType.INAPP -> acknowledgeHelper.consumePurchase(purchaseToken)
            ProductType.SUBS -> acknowledgeHelper.acknowledgePurchase(purchaseToken)
            ProductType.UNKNOWN -> {
                Log.w(TAG, "Unknown product type, attempting acknowledge")
                acknowledgeHelper.acknowledgePurchase(purchaseToken)
            }
        }
        if (!success) {
            Log.e(TAG, "Failed to finish transaction for token: $purchaseToken")
        }
    }

    companion object {
        private const val TAG = "MyAppThroughRC"
    }
}
