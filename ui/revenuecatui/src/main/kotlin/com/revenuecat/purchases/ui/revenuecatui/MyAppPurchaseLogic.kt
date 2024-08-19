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
 * `MyAppPurchaseLogicWithCallback`.
 */
interface MyAppPurchaseLogic {
    /**
     * Performs an in-app purchase for the specified package.
     *
     * If a purchase is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param activity The current Android `Activity` triggering the purchase.
     * @param rcPackage The package representing the in-app product that the user intends to purchase.
     * @return A `MyAppPurchaseResult` object containing the outcome of the purchase operation.
     */
    suspend fun performPurchase(activity: Activity, rcPackage: Package): MyAppPurchaseResult

    /**
     * Restores previously completed purchases for the given customer.
     *
     * If restoration is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param customerInfo An object containing information about the customer.
     * @return A `MyAppRestoreResult` object containing the outcome of the restoration process.
     */
    suspend fun performRestore(customerInfo: CustomerInfo): MyAppRestoreResult
}

/**
 * Abstract class extending `MyAppPurchaseLogic`, providing methods for handling in-app purchases and restorations
 * with completion callbacks rather than co-routines.
 *
 * If you prefer to implement custom purchase and restore logic with coroutines, please implement
 * `MyAppPurchaseLogic` directly.
 */
abstract class MyAppPurchaseLogicWithCallback : MyAppPurchaseLogic {

    /**
     * Performs an in-app purchase for the specified package with a completion callback.
     *
     * If a purchase is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param activity The current Android `Activity` triggering the purchase.
     * @param rcPackage The package representing the in-app product that the user intends to purchase.
     * @param completion A callback function that receives a `MyAppPurchaseResult` object containing the outcome of the
     * purchase operation.
     */
    abstract fun performPurchaseWithCompletion(
        activity: Activity,
        rcPackage: Package,
        completion: (MyAppPurchaseResult) -> Unit,
    )

    /**
     * Restores previously completed purchases for the given customer with a completion callback.
     *
     * If restoration is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param completion A callback function that receives a `MyAppRestoreResult` object containing the outcome of the
     * restoration process.
     */
    abstract fun performRestoreWithCompletion(completion: (MyAppRestoreResult) -> Unit)

    /**
     * This method is called by RevenueCat, which in turn calls `performPurchaseWithCompletion` where your app's
     * custom purchase logic is performed.
     */
    final override suspend fun performPurchase(activity: Activity, rcPackage: Package): MyAppPurchaseResult =
        suspendCoroutine { continuation ->
            performPurchaseWithCompletion(activity, rcPackage) { result ->
                continuation.resume(result)
            }
        }

    /**
     * This method is called by RevenueCat, which in turn calls `performRestoreWithCompletion` where your app's
     * custom purchase logic is performed.
     */
    final override suspend fun performRestore(customerInfo: CustomerInfo): MyAppRestoreResult =
        suspendCoroutine { continuation ->
            performRestoreWithCompletion { result ->
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
     * @property error Details of the error that occurred. If provided, an error dialog will be shown to the user.
     */
    data class Error(val errorDetails: PurchasesError? = null) : MyAppPurchaseResult
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
     * @property error Details of the error that occurred. If provided an error dialog will be shown to the user.
     */
    data class Error(val errorDetails: PurchasesError? = null) : MyAppRestoreResult
}
