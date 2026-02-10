package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI

/**
 * Interface for providing custom paywall behavior handlers.
 *
 * Implement this interface to provide [PaywallListener] and/or [PurchaseLogic] for
 * a specific paywall. You only need to override the properties for the handlers you
 * want to provide.
 *
 * This is typically created by a [CustomPaywallHandlerFactory] and allows you to
 * provide different handlers based on the offering being displayed.
 *
 * Example:
 * ```kotlin
 * class MyCustomHandler : CustomPaywallHandler {
 *     override val paywallListener = object : PaywallListener {
 *         override fun onPurchaseStarted(rcPackage: Package) {
 *             Log.d("Paywall", "Purchase started")
 *         }
 *     }
 *
 *     override val purchaseLogic = object : PurchaseLogic {
 *         override suspend fun performPurchase(
 *             activity: Activity,
 *             rcPackage: Package
 *         ): PurchaseLogicResult {
 *             // Custom purchase logic
 *             return PurchaseLogicResult.Success
 *         }
 *
 *         override suspend fun performRestore(
 *             customerInfo: CustomerInfo
 *         ): PurchaseLogicResult {
 *             // Custom restore logic
 *             return PurchaseLogicResult.Success
 *         }
 *     }
 * }
 * ```
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
interface CustomPaywallHandler {
    /**
     * The [PaywallListener] to use for this paywall.
     * Return null if you don't want to provide a listener for this paywall.
     */
    val paywallListener: PaywallListener?
        get() = null

    /**
     * The [PurchaseLogic] to use for this paywall.
     * Return null if you don't want to provide custom purchase logic for this paywall.
     *
     * Note: [PurchaseLogic] is only used when [com.revenuecat.purchases.Purchases.purchasesAreCompletedBy] is set to
     * [com.revenuecat.purchases.PurchasesAreCompletedBy.MY_APP]. If you provide this when using
     * [com.revenuecat.purchases.PurchasesAreCompletedBy.REVENUECAT], it will be ignored.
     */
    val purchaseLogic: PurchaseLogic?
        get() = null
}
