package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Interface for handling in-app purchases and restorations directly by the application rather than by RevenueCat.
 * These suspend methods are called by a RevenueCat Paywall in order to execute you app's custom purchase/restore code.
 * These functions are only called when `Purchases.purchasesAreCompletedBy` is set to `MY_APP`.
 *
 * If you prefer to implement custom purchase and restore logic with completion handlers, please implement
 * `PurchaseLogicWithCallback`.
 */
interface PurchaseLogic {
    /**
     * Performs an in-app purchase for the specified package.
     *
     * If a purchase is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param activity The current Android `Activity` triggering the purchase.
     * @param rcPackage The package representing the in-app product that the user intends to purchase.
     * @return A `PurchaseLogicPurchaseResult` object containing the outcome of the purchase operation.
     */
    suspend fun performPurchase(activity: Activity, rcPackage: Package): PurchaseLogicPurchaseResult

    /**
     * Restores previously completed purchases for the given customer.
     *
     * If restoration is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param customerInfo An object containing information about the customer.
     * @return A `PurchaseLogicRestoreResult` object containing the outcome of the restoration process.
     */
    suspend fun performRestore(customerInfo: CustomerInfo): PurchaseLogicRestoreResult
}

/**
 * Abstract class extending `PurchaseLogic`, providing methods for handling in-app purchases and restorations
 * with completion callbacks rather than co-routines.
 *
 * If you prefer to implement custom purchase and restore logic with coroutines, please implement
 * `PurchaseLogic` directly.
 */
abstract class PurchaseLogicWithCallback : PurchaseLogic {

    /**
     * Performs an in-app purchase for the specified package with a completion callback.
     *
     * If a purchase is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param activity The current Android `Activity` triggering the purchase.
     * @param rcPackage The package representing the in-app product that the user intends to purchase.
     * @param completion A callback function that receives a `PurchaseLogicPurchaseResult` object containing the outcome
     * of the purchase operation.
     */
    abstract fun performPurchaseWithCompletion(
        activity: Activity,
        rcPackage: Package,
        completion: (PurchaseLogicPurchaseResult) -> Unit,
    )

    /**
     * Restores previously completed purchases for the given customer with a completion callback.
     *
     * If restoration is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param customerInfo An object containing information about the customer.
     * @param completion A callback function that receives a `PurchaseLogicRestoreResult` object containing the outcome
     * of the restoration process.
     */
    abstract fun performRestoreWithCompletion(customerInfo: CustomerInfo, completion: (PurchaseLogicRestoreResult) -> Unit)

    /**
     * This method is called by RevenueCat, which in turn calls `performPurchaseWithCompletion` where your app's
     * custom purchase logic is performed.
     */
    final override suspend fun performPurchase(activity: Activity, rcPackage: Package): PurchaseLogicPurchaseResult =
        suspendCoroutine { continuation ->
            performPurchaseWithCompletion(activity, rcPackage) { result ->
                continuation.resume(result)
            }
        }

    /**
     * This method is called by RevenueCat, which in turn calls `performRestoreWithCompletion` where your app's
     * custom purchase logic is performed.
     */
    final override suspend fun performRestore(customerInfo: CustomerInfo): PurchaseLogicRestoreResult =
        suspendCoroutine { continuation ->
            performRestoreWithCompletion(customerInfo) { result ->
                continuation.resume(result)
            }
        }
}

/**
 * Represents the result of a purchase attempt made by custom app-based code (not RevenueCat).
 */
sealed interface PurchaseLogicPurchaseResult {
    /**
     * Indicates a successful purchase.
     *
     */
    object Success : PurchaseLogicPurchaseResult

    /**
     * Indicates the purchase was cancelled.
     */
    object Cancellation : PurchaseLogicPurchaseResult

    /**
     * Indicates an error occurred during the purchase attempt.
     *
     * @property error Details of the error that occurred. If provided, an error dialog will be shown to the user.
     */
    data class Error(val errorDetails: PurchasesError? = null) : PurchaseLogicPurchaseResult
}

/**
 * Represents the result of a restore purchases attempt.
 */
sealed interface PurchaseLogicRestoreResult {
    /**
     * Indicates a successful restore operation.
     */
    object Success : PurchaseLogicRestoreResult

    /**
     * Indicates an error occurred during the restore attempt.
     *
     * @property error Details of the error that occurred. If provided an error dialog will be shown to the user.
     */
    data class Error(val errorDetails: PurchasesError? = null) : PurchaseLogicRestoreResult
}
