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
 * @deprecated Use [PaywallPurchaseLogic] instead, which provides a [PaywallPurchaseLogicParams] with support for
 * product changes (upgrades/downgrades) and subscription offers.
 */
/**
 * Interface for handling in-app purchases and restorations directly by the application rather than by RevenueCat.
 * These suspend methods are called by a RevenueCat Paywall in order to execute your app's custom purchase/restore
 * code. These functions are only called when `Purchases.purchasesAreCompletedBy` is set to `MY_APP`.
 *
 * This interface provides [PaywallPurchaseLogicParams] containing the package to purchase along with additional
 * information such as product change details (for upgrades/downgrades) and the specific subscription option
 * (offer) configured in the paywall. The params object is designed to be extensible for future additions.
 *
 * If you prefer to implement custom purchase and restore logic with completion handlers, please use
 * [PaywallPurchaseLogicWithCallback].
 */
public interface PaywallPurchaseLogic {
    /**
     * Performs an in-app purchase with the given purchase params.
     *
     * The [PaywallPurchaseLogicParams] contains the package to purchase along with product change information
     * and the specific subscription option (offer) configured in the paywall.
     *
     * If a purchase is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param activity The current Android `Activity` triggering the purchase.
     * @param params The purchase params containing the package, product change information, and offer details.
     * @return A `PurchaseLogicResult` object containing the outcome of the purchase operation.
     */
    public suspend fun performPurchase(
        activity: Activity,
        params: PaywallPurchaseLogicParams,
    ): PurchaseLogicResult

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

@Suppress("DEPRECATION")
@Deprecated(
    message = "Use PaywallPurchaseLogic instead for product change and offer support.",
    replaceWith = ReplaceWith("PaywallPurchaseLogic"),
)
public interface PurchaseLogic : PaywallPurchaseLogic {
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

    override suspend fun performPurchase(activity: Activity, params: PaywallPurchaseLogicParams): PurchaseLogicResult =
        performPurchase(activity, params.rcPackage)
}

/**
 * Parameters provided to [PaywallPurchaseLogic] when a paywall initiates a purchase.
 *
 * Contains the package to purchase along with information about product changes (upgrades/downgrades)
 * and the specific subscription option (offer) to use, as configured in the paywall.
 *
 * Use [Builder] to create an instance:
 * ```kotlin
 * val params = PaywallPurchaseLogicParams.Builder(rcPackage)
 *     .oldProductId("com.example.old_product")
 *     .replacementMode(GoogleReplacementMode.CHARGE_PRORATED_PRICE)
 *     .subscriptionOption(subscriptionOption)
 *     .build()
 * ```
 *
 * @property rcPackage The package representing the in-app product that the user intends to purchase.
 * @property oldProductId The product ID of the currently active subscription being replaced when the user is
 * upgrading or downgrading. Null if this is a new purchase rather than a product change.
 * @property replacementMode The replacement mode to use for this product change, as configured in the paywall.
 * Null if this is a new purchase or the store does not support replacement modes.
 * @property subscriptionOption The specific subscription option (offer) to use for this purchase, as configured
 * in the paywall. Null if no specific offer is configured or the product is not a subscription.
 */
@Poko
public class PaywallPurchaseLogicParams internal constructor(
    public val rcPackage: Package,
    internal val productChange: ProductChange?,
    public val subscriptionOption: SubscriptionOption?,
) {
    /**
     * The product ID of the currently active subscription being replaced, when the user is upgrading
     * or downgrading an existing subscription. Null if this is a new purchase.
     */
    public val oldProductId: String? get() = productChange?.oldProductId

    /**
     * The replacement mode to use for this product change, as configured in the paywall.
     * For Google Play, this will be a [com.revenuecat.purchases.models.GoogleReplacementMode].
     * Null if this is a new purchase or the store does not support replacement modes.
     */
    public val replacementMode: ReplacementMode? get() = productChange?.replacementMode

    /**
     * Builder for creating [PaywallPurchaseLogicParams].
     *
     * @param rcPackage The package representing the in-app product that the user intends to purchase.
     */
    public class Builder(private val rcPackage: Package) {
        private var oldProductId: String? = null
        private var replacementMode: ReplacementMode? = null
        private var subscriptionOption: SubscriptionOption? = null

        /**
         * Sets the product ID of the currently active subscription being replaced
         * (for upgrades/downgrades).
         */
        public fun oldProductId(oldProductId: String): Builder = apply {
            this.oldProductId = oldProductId
        }

        /**
         * Sets the replacement mode for this product change.
         * For Google Play, use [com.revenuecat.purchases.models.GoogleReplacementMode].
         */
        public fun replacementMode(replacementMode: ReplacementMode?): Builder = apply {
            this.replacementMode = replacementMode
        }

        /**
         * Sets the specific subscription option (offer) to use for this purchase.
         */
        public fun subscriptionOption(subscriptionOption: SubscriptionOption?): Builder = apply {
            this.subscriptionOption = subscriptionOption
        }

        /**
         * Builds the [PaywallPurchaseLogicParams] instance.
         */
        public fun build(): PaywallPurchaseLogicParams = PaywallPurchaseLogicParams(
            rcPackage = rcPackage,
            productChange = oldProductId?.let { ProductChange(it, replacementMode) },
            subscriptionOption = subscriptionOption,
        )
    }
}

@Poko
internal class ProductChange(
    val oldProductId: String,
    val replacementMode: ReplacementMode?,
)

/**
 * Abstract class extending [PurchaseLogic], providing methods for handling in-app purchases and restorations
 * with completion callbacks rather than co-routines.
 *
 * If you prefer to implement custom purchase and restore logic with coroutines, please implement
 * [PurchaseLogic] directly.
 *
 * @deprecated Use [PaywallPurchaseLogicWithCallback] instead, which provides a [PaywallPurchaseLogicParams] with
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
     * Performs an in-app purchase with additional purchase params, using a completion callback.
     *
     * The [PaywallPurchaseLogicParams] contains the package to purchase along with product change information
     * and the specific subscription option (offer) configured in the paywall.
     *
     * If a purchase is successful, `syncPurchases` will automatically be called by RevenueCat to update our
     * database. However, if you are using Amazon's store, you must call `syncAmazonPurchase` in your code.
     *
     * @param activity The current Android `Activity` triggering the purchase.
     * @param params The purchase params containing the package, product change information, and offer details.
     * @param completion A callback function that receives a `PurchaseLogicResult` object containing the outcome
     * of the purchase operation.
     */
    public abstract fun performPurchaseWithCompletion(
        activity: Activity,
        params: PaywallPurchaseLogicParams,
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
        params: PaywallPurchaseLogicParams,
    ): PurchaseLogicResult =
        suspendCoroutine { continuation ->
            performPurchaseWithCompletion(activity, params) { result ->
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
