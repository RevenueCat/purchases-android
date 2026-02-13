package com.revenuecat.rcttester.purchasing

import android.app.Activity
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic

/**
 * Purchase manager for the standard RevenueCat integration mode.
 *
 * In this mode:
 * - `purchasesAreCompletedBy` is set to `REVENUECAT`
 * - RevenueCat handles all purchase and restore operations
 * - Paywalls don't need custom purchase logic
 */
class RevenueCatPurchaseManager : PurchaseManager {

    override val purchaseLogic: PurchaseLogic? = null

    override suspend fun purchase(activity: Activity, rcPackage: Package): PurchaseOperationResult {
        return try {
            val purchaseParams = PurchaseParams.Builder(activity, rcPackage).build()
            val result = Purchases.sharedInstance.awaitPurchase(purchaseParams)
            PurchaseOperationResult.Success(result.customerInfo)
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) {
                PurchaseOperationResult.UserCancelled
            } else {
                PurchaseOperationResult.Failure(e.message ?: "Unknown error")
            }
        }
    }
}
