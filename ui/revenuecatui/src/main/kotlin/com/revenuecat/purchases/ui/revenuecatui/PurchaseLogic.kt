package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ReplacementMode
import com.revenuecat.purchases.models.SubscriptionOption
import dev.drewhamilton.poko.Poko
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Interface for handling in-app purchases and restorations directly by the application rather than by RevenueCat.
 * These suspend methods are called by a RevenueCat Paywall in order to execute you app's custom purchase/restore code.
 * These functions are only called when `Purchases.purchasesAreCompletedBy` is set to `MY_APP`.
 *
 * If you prefer to implement custom purchase and restore logic with completion handlers, please implement
 * `PurchaseLogicWithCallback`.
 *
 * @deprecated Use [PaywallPurchaseLogic] instead, which provides a [PaywallPurchaseContext] with support for
 * product changes (upgrades/downgrades) and subscription offers.
 */
@Deprecated(
    message = "Use PaywallPurchaseLogic instead for product change and offer support.",
    replaceWith = ReplaceWith("PaywallPurchaseLogic"),
)
public interface PurchaseLogic {
    /**
     * Performs an in-app purchase for the specified package.
     *
     * If a purchase is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param activity The current Android `Activity` triggering the purchase.
     * @param rcPackage The package representing the in-app product that the user intends to purchase.
     * @return A `PurchaseLogicResult` object containing the outcome of the purchase operation.
     */
    public suspend fun performPurchase(activity: Activity, rcPackage: Package): PurchaseLogicResult

    /**
     * Restores previously completed purchases for the given customer.
     *
     * If restoration is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param customerInfo An object containing information about the customer.
     * @return A `PurchaseLogicResult` object containing the outcome of the restoration process.
     */
    public suspend fun performRestore(customerInfo: CustomerInfo): PurchaseLogicResult
}

/**
 * Interface for handling in-app purchases and restorations directly by the application rather than by RevenueCat.
 * These suspend methods are called by a RevenueCat Paywall in order to execute your app's custom purchase/restore
 * code. These functions are only called when `Purchases.purchasesAreCompletedBy` is set to `MY_APP`.
 *
 * This interface provides a [PaywallPurchaseContext] containing the package to purchase along with additional
 * context such as product change information (for upgrades/downgrades) and the specific subscription option
 * (offer) configured in the paywall. The context object is designed to be extensible for future additions.
 *
 * If you prefer to implement custom purchase and restore logic with completion handlers, please use
 * [PaywallPurchaseLogicWithCallback].
 */
@Suppress("DEPRECATION")
public interface PaywallPurchaseLogic : PurchaseLogic {
    /**
     * Performs an in-app purchase with the given purchase context.
     *
     * The [PaywallPurchaseContext] contains the package to purchase along with product change information
     * and the specific subscription option (offer) configured in the paywall.
     *
     * If a purchase is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param activity The current Android `Activity` triggering the purchase.
     * @param context The purchase context containing the package, product change information, and offer details.
     * @return A `PurchaseLogicResult` object containing the outcome of the purchase operation.
     */
    public suspend fun performPurchase(
        activity: Activity,
        context: PaywallPurchaseContext,
    ): PurchaseLogicResult

    override suspend fun performPurchase(activity: Activity, rcPackage: Package): PurchaseLogicResult =
        performPurchase(activity, PaywallPurchaseContext(rcPackage, productChange = null, subscriptionOption = null))
}

/**
 * Additional context provided to [PaywallPurchaseLogic] when a paywall initiates a purchase.
 *
 * Contains the package to purchase along with information about product changes (upgrades/downgrades)
 * and the specific subscription option (offer) to use, as configured in the paywall.
 *
 * @property rcPackage The package representing the in-app product that the user intends to purchase.
 * @property productChange Product change information when the user is upgrading or downgrading an existing
 * subscription. Null if this is a new purchase rather than a product change.
 * @property subscriptionOption The specific subscription option (offer) to use for this purchase, as configured
 * in the paywall. Null if no specific offer is configured or the product is not a subscription.
 */
@Poko
public class PaywallPurchaseContext(
    public val rcPackage: Package,
    public val productChange: ProductChange?,
    public val subscriptionOption: SubscriptionOption?,
)

/**
 * Contains information about a subscription product change (upgrade or downgrade).
 *
 * When a user with an active subscription purchases a different subscription product through a paywall,
 * this object provides the details needed to set up the subscription change.
 *
 * @property oldProductId The product ID of the currently active subscription being replaced.
 * @property replacementMode The replacement mode to use for this product change, as configured in the paywall.
 * For Google Play, this will be a [com.revenuecat.purchases.models.GoogleReplacementMode].
 * Null for stores that do not support replacement modes (e.g., Amazon).
 */
@Poko
public class ProductChange(
    public val oldProductId: String,
    public val replacementMode: ReplacementMode?,
)

/**
 * Abstract class extending [PurchaseLogic], providing methods for handling in-app purchases and restorations
 * with completion callbacks rather than co-routines.
 *
 * If you prefer to implement custom purchase and restore logic with coroutines, please implement
 * [PurchaseLogic] directly.
 *
 * @deprecated Use [PaywallPurchaseLogicWithCallback] instead, which provides a [PaywallPurchaseContext] with
 * support for product changes (upgrades/downgrades) and subscription offers.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Use PaywallPurchaseLogicWithCallback instead for product change and offer support.",
    replaceWith = ReplaceWith("PaywallPurchaseLogicWithCallback"),
)
public abstract class PurchaseLogicWithCallback : PurchaseLogic {

    /**
     * Performs an in-app purchase for the specified package with a completion callback.
     *
     * If a purchase is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param activity The current Android `Activity` triggering the purchase.
     * @param rcPackage The package representing the in-app product that the user intends to purchase.
     * @param completion A callback function that receives a `PurchaseLogicResult` object containing the outcome
     * of the purchase operation.
     */
    public abstract fun performPurchaseWithCompletion(
        activity: Activity,
        rcPackage: Package,
        completion: (PurchaseLogicResult) -> Unit,
    )

    /**
     * Restores previously completed purchases for the given customer with a completion callback.
     *
     * If restoration is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param customerInfo An object containing information about the customer.
     * @param completion A callback function that receives a `PurchaseLogicResult` object containing the outcome
     * of the restoration process.
     */
    public abstract fun performRestoreWithCompletion(
        customerInfo: CustomerInfo,
        completion: (PurchaseLogicResult) -> Unit,
    )

    /**
     * This method is called by RevenueCat, which in turn calls `performPurchaseWithCompletion` where your app's
     * custom purchase logic is performed.
     */
    final override suspend fun performPurchase(activity: Activity, rcPackage: Package): PurchaseLogicResult =
        suspendCoroutine { continuation ->
            performPurchaseWithCompletion(activity, rcPackage) { result ->
                continuation.resume(result)
            }
        }

    /**
     * This method is called by RevenueCat, which in turn calls `performRestoreWithCompletion` where your app's
     * custom restore logic is performed.
     */
    final override suspend fun performRestore(customerInfo: CustomerInfo): PurchaseLogicResult =
        suspendCoroutine { continuation ->
            performRestoreWithCompletion(customerInfo) { result ->
                continuation.resume(result)
            }
        }
}

/**
 * Abstract class extending [PaywallPurchaseLogic], providing methods for handling in-app purchases and
 * restorations with completion callbacks rather than co-routines.
 *
 * If you prefer to implement custom purchase and restore logic with coroutines, please implement
 * [PaywallPurchaseLogic] directly.
 */
public abstract class PaywallPurchaseLogicWithCallback : PaywallPurchaseLogic {

    /**
     * Performs an in-app purchase with additional purchase context, using a completion callback.
     *
     * The [PaywallPurchaseContext] contains the package to purchase along with product change information
     * and the specific subscription option (offer) configured in the paywall.
     *
     * If a purchase is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param activity The current Android `Activity` triggering the purchase.
     * @param context The purchase context containing the package, product change information, and offer details.
     * @param completion A callback function that receives a `PurchaseLogicResult` object containing the outcome
     * of the purchase operation.
     */
    public abstract fun performPurchaseWithCompletion(
        activity: Activity,
        context: PaywallPurchaseContext,
        completion: (PurchaseLogicResult) -> Unit,
    )

    /**
     * Restores previously completed purchases for the given customer with a completion callback.
     *
     * If restoration is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param customerInfo An object containing information about the customer.
     * @param completion A callback function that receives a `PurchaseLogicResult` object containing the outcome
     * of the restoration process.
     */
    public abstract fun performRestoreWithCompletion(
        customerInfo: CustomerInfo,
        completion: (PurchaseLogicResult) -> Unit,
    )

    /**
     * This method is called by RevenueCat, which in turn calls `performPurchaseWithCompletion` where your app's
     * custom purchase logic is performed.
     */
    final override suspend fun performPurchase(
        activity: Activity,
        context: PaywallPurchaseContext,
    ): PurchaseLogicResult =
        suspendCoroutine { continuation ->
            performPurchaseWithCompletion(activity, context) { result ->
                continuation.resume(result)
            }
        }

    /**
     * This method is called by RevenueCat, which in turn calls `performRestoreWithCompletion` where your app's
     * custom restore logic is performed.
     */
    final override suspend fun performRestore(customerInfo: CustomerInfo): PurchaseLogicResult =
        suspendCoroutine { continuation ->
            performRestoreWithCompletion(customerInfo) { result ->
                continuation.resume(result)
            }
        }
}

/**
 * Represents the result of a purchase attempt made by custom app-based code (not RevenueCat).
 */
public sealed interface PurchaseLogicResult {
    /**
     * Indicates a successful purchase or restore.
     */
    public object Success : PurchaseLogicResult

    /**
     * Indicates the purchase or restore was cancelled.
     */
    public object Cancellation : PurchaseLogicResult

    /**
     * Indicates an error occurred during the purchase or restore attempt.
     *
     * @property error Details of the error that occurred. If provided, an error dialog will be shown to the user.
     */
    @Poko
    public class Error(public val errorDetails: PurchasesError? = null) :
        PurchaseLogicResult
}
