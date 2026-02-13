package com.revenuecat.rcttester.purchasing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.rcttester.config.PurchaseLogicType
import com.revenuecat.rcttester.config.PurchasesCompletedByType
import com.revenuecat.rcttester.config.SDKConfiguration

/**
 * Result type for purchase operations initiated outside of paywalls.
 */
sealed class PurchaseOperationResult {
    data class Success(val customerInfo: CustomerInfo? = null) : PurchaseOperationResult()

    /**
     * Success from the app's custom BillingClient flow (USING_BILLING_CLIENT_DIRECTLY).
     * The sample can show different context, e.g. that entitlements will update after sync.
     */
    data object SuccessCustomImplementation : PurchaseOperationResult()

    data object UserCancelled : PurchaseOperationResult()
    data object Pending : PurchaseOperationResult()
    data class Failure(val error: String) : PurchaseOperationResult()
}

/**
 * Protocol that abstracts how the app interacts with the RevenueCat SDK for purchases.
 *
 * The app uses this interface agnostically without knowing the underlying integration mode.
 */
interface PurchaseManager {

    /**
     * Returns the [PurchaseLogic] to be passed to paywall options.
     *
     * - Returns `null` when `purchasesAreCompletedBy == REVENUECAT`, meaning paywalls
     *   use RevenueCat's built-in purchase handling.
     * - Returns a configured [PurchaseLogic] when `purchasesAreCompletedBy == MY_APP`,
     *   allowing paywalls to delegate purchases to this manager.
     */
    val purchaseLogic: PurchaseLogic?

    /**
     * Purchases a package directly (used for purchase buttons outside of paywalls).
     */
    suspend fun purchase(activity: Activity, rcPackage: Package): PurchaseOperationResult
}

/**
 * Creates the appropriate [PurchaseManager] implementation based on the SDK configuration.
 */
fun createPurchaseManager(context: Context, configuration: SDKConfiguration): PurchaseManager {
    return when (configuration.purchasesAreCompletedBy) {
        PurchasesCompletedByType.REVENUECAT -> RevenueCatPurchaseManager()
        PurchasesCompletedByType.MY_APP -> {
            when (configuration.purchaseLogic) {
                PurchaseLogicType.THROUGH_REVENUECAT -> {
                    val billingClient = createBillingClient(context) { _, _ -> }
                    PurchasesAreCompletedByMyAppThroughRevenueCatPurchaseManager(billingClient)
                }
                PurchaseLogicType.USING_BILLING_CLIENT_DIRECTLY -> {
                    lateinit var manager:
                        PurchasesAreCompletedByMyAppUsingBillingClientPurchaseManager
                    val billingClient = createBillingClient(context) { billingResult, purchases ->
                        manager.onPurchasesUpdated(billingResult, purchases)
                    }
                    manager = PurchasesAreCompletedByMyAppUsingBillingClientPurchaseManager(
                        billingClient,
                    )
                    manager
                }
            }
        }
    }
}

private fun createBillingClient(context: Context, listener: PurchasesUpdatedListener): BillingClient {
    return BillingClient.newBuilder(context)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build(),
        )
        .setListener(listener)
        .build()
}
