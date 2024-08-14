package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface MyAppPurchaseLogic {
    suspend fun performPurchase(activity: Activity, rcPackage: Package): MyAppPurchaseResult
    suspend fun performRestore(customerInfo: CustomerInfo): MyAppRestoreResult
}

abstract class MyAppPurchaseLogicCompletion : MyAppPurchaseLogic {

    abstract fun performPurchase(activity: Activity, rcPackage: Package, completion: (MyAppPurchaseResult) -> Unit)

    abstract fun performRestore(completion: (MyAppRestoreResult) -> Unit)

    final override suspend fun performPurchase(activity: Activity, rcPackage: Package): MyAppPurchaseResult =
        suspendCoroutine { continuation ->
            performPurchase(activity, rcPackage) { result ->
                continuation.resume(result)
            }
        }

    final override suspend fun performRestore(customerInfo: CustomerInfo): MyAppRestoreResult =
        suspendCoroutine { continuation ->
            performRestore { result ->
                continuation.resume(result)
            }
        }
}

/**
 * Represents the result of a purchase attempt made by custom app-based code (not RevenueCat).
 */
sealed interface MyAppPurchaseResult {
    /**
     * Indicates a successful purchase.
     *
     * @property purchase The purchase details if available. This object is only used to call the
     * `PaywallListener` `onPurchaseCompleted` callback. If this callback is not needed, it does not need to be
     * provided, and the callback will not be called.
     */
    object Success : MyAppPurchaseResult

    /**
     * Indicates the purchase was cancelled.
     */
    object Cancellation : MyAppPurchaseResult

    /**
     * Indicates an error occurred during the purchase attempt.
     *
     * @property error Details of the error that occurred.
     */
    data class Error(val error: PurchasesError) : MyAppPurchaseResult
}

/**
 * Represents the result of a restore purchases attempt.
 */
sealed interface MyAppRestoreResult {
    /**
     * Indicates a successful restore operation.
     */
    object Success : MyAppRestoreResult

    /**
     * Indicates an error occurred during the restore attempt.
     *
     * @property error Details of the error that occurred.
     */
    data class Error(val error: PurchasesError) : MyAppRestoreResult
}

