package com.revenuecat.purchases

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.infoLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.interfaces.Callback
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.BillingFeature
import com.revenuecat.purchases.models.InAppMessageType
import com.revenuecat.purchases.models.MyAppPurchaseLogic
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.strings.BillingStrings
import com.revenuecat.purchases.strings.ConfigureStrings
import java.net.URL

/**
 * Entry point for Purchases. It should be instantiated as soon as your app has a unique user id
 * for your user. This can be when a user logs in if you have accounts or on launch if you can
 * generate a random user identifier.
 * Make sure you follow the [quickstart](https://docs.revenuecat.com/docs/getting-started-1)
 * guide to setup your RevenueCat account.
 * @warning Only one instance of Purchases should be instantiated at a time!
 */
@Suppress("TooManyFunctions")
class Purchases internal constructor(
    @get:JvmSynthetic internal val purchasesOrchestrator: PurchasesOrchestrator,
) {

    /**
     * The passed in or generated app user ID
     */
    val appUserID: String
        @Synchronized get() = purchasesOrchestrator.appUserID

    /**
     * The listener is responsible for handling changes to customer information.
     * Make sure [removeUpdatedCustomerInfoListener] is called when the listener needs to be destroyed.
     */
    var updatedCustomerInfoListener: UpdatedCustomerInfoListener?
        @Synchronized get() = purchasesOrchestrator.updatedCustomerInfoListener

        @Synchronized set(value) {
            purchasesOrchestrator.updatedCustomerInfoListener = value
        }

    // region Public Methods

    /**
     * Fetch the configured offerings for this users. Offerings allows you to configure your in-app
     * products vis RevenueCat and greatly simplifies management. See
     * [the guide](https://docs.revenuecat.com/offerings) for more info.
     *
     * Offerings will be fetched and cached on instantiation so that, by the time they are needed,
     * your prices are loaded for your purchase flow. Time is money.
     *
     * @param [listener] Called when offerings are available. Called immediately if offerings are cached.
     */
    fun getOfferings(
        listener: ReceiveOfferingsCallback,
    ) {
        purchasesOrchestrator.getOfferings(listener)
    }

    /**
     * Gets the StoreProduct(s) for the given list of product ids for all product types.
     * @param [productIds] List of productIds
     * @param [callback] Response callback
     */
    fun getProducts(
        productIds: List<String>,
        callback: GetStoreProductsCallback,
    ) {
        getProducts(productIds, null, callback)
    }

    /**
     * Gets the StoreProduct(s) for the given list of product ids of type [type]
     * @param [productIds] List of productIds
     * @param [type] A product type to filter (no filtering applied if null)
     * @param [callback] Response callback
     */
    fun getProducts(
        productIds: List<String>,
        type: ProductType? = null,
        callback: GetStoreProductsCallback,
    ) {
        purchasesOrchestrator.getProducts(productIds, type, callback)
    }

    /**
     * Initiate a purchase with the given [PurchaseParams].
     * Initialized with an [Activity] either a [Package], [StoreProduct], or [SubscriptionOption].
     *
     * If a [Package] or [StoreProduct] is used to build the [PurchaseParams], the [defaultOption] will be purchased.
     * [defaultOption] is selected via the following logic:
     *   - Filters out offers with "rc-ignore-offer" tag
     *   - Uses [SubscriptionOption] with the longest free trial or cheapest first phase
     *   - Falls back to use base plan
     *
     *   @params [purchaseParams] The parameters configuring the purchase. See [PurchaseParams.Builder] for options.
     *   @params [callback] The PurchaseCallback that will be called when purchase completes.
     */
    fun purchase(
        purchaseParams: PurchaseParams,
        callback: PurchaseCallback,
    ) {
        purchasesOrchestrator.purchase(purchaseParams, callback)
    }

    /**
     * Call close when you are done with this instance of Purchases
     */
    fun close() {
        purchasesOrchestrator.close()
    }

    /**
     * Call this when you are finished using the [UpdatedCustomerInfoListener]. You should call this
     * to avoid memory leaks.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun removeUpdatedCustomerInfoListener() {
        purchasesOrchestrator.removeUpdatedCustomerInfoListener()
    }

    /**
     * Google Play only, no-op for Amazon.
     * Displays the specified in-app message types to the user as a snackbar if there are any available to be shown.
     * If [PurchasesConfiguration.showInAppMessagesAutomatically] is enabled, this will be done
     * automatically on each Activity's onStart.
     *
     * For more info: https://rev.cat/googleplayinappmessaging
     */
    @JvmOverloads
    fun showInAppMessagesIfNeeded(
        activity: Activity,
        inAppMessageTypes: List<InAppMessageType> = listOf(InAppMessageType.BILLING_ISSUES),
    ) {
        purchasesOrchestrator.showInAppMessagesIfNeeded(activity, inAppMessageTypes)
    }

    /**
     * Restores purchases made with the current Play Store account for the current user.
     * This method will post all purchases associated with the current Play Store account to
     * RevenueCat and become associated with the current `appUserID`. If the receipt token is being
     * used by an existing user, the current `appUserID` will be aliased together with the
     * `appUserID` of the existing user. Going forward, either `appUserID` will be able to reference
     * the same user.
     *
     * You shouldn't use this method if you have your own account system. In that case
     * "restoration" is provided by your app passing the same `appUserId` used to purchase originally.
     * @param [callback] The listener that will be called when purchase restore completes.
     */
    fun restorePurchases(
        callback: ReceiveCustomerInfoCallback,
        myAppPurchaseLogic: MyAppPurchaseLogic?
    ) {
        purchasesOrchestrator.restorePurchases(callback, myAppPurchaseLogic)
    }

    /**
     * Updates the current appUserID to a new one, without associating the two.
     *
     * Important: This method is only available in Custom Entitlements Computation mode.
     * Tokens posted by the SDK to the RevenueCat backend after calling this method will be sent
     * with the newAppUserID.
     */
    fun switchUser(newAppUserID: String) {
        purchasesOrchestrator.switchUser(newAppUserID)
    }
    //endregion

    // region Static
    companion object {

        /**
         * DO NOT MODIFY. This is used internally by the Hybrid SDKs to indicate which platform is
         * being used
         */
        @JvmStatic
        var platformInfo: PlatformInfo
            get() = PurchasesOrchestrator.platformInfo
            set(value) { PurchasesOrchestrator.platformInfo = value }

        /**
         * Configure log level. Useful for debugging issues with the lovely team @RevenueCat
         * By default, LogLevel.DEBUG in debug builds, and LogLevel.INFO in release builds.
         */
        @JvmStatic
        var logLevel: LogLevel
            get() = PurchasesOrchestrator.logLevel
            set(value) {
                PurchasesOrchestrator.logLevel = value
            }

        /**
         * Set a custom log handler for redirecting logs to your own logging system.
         * Defaults to [android.util.Log].
         *
         * By default, this sends info, warning, and error messages.
         * If you wish to receive Debug level messages, see [debugLogsEnabled].
         */
        @JvmStatic
        var logHandler: LogHandler
            @Synchronized get() = PurchasesOrchestrator.logHandler

            @Synchronized set(value) {
                PurchasesOrchestrator.logHandler = value
            }

        @JvmSynthetic
        internal var backingFieldSharedInstance: Purchases? = null

        /**
         * Singleton instance of Purchases. [configure] will set this
         * @return A previously set singleton Purchases instance
         * @throws UninitializedPropertyAccessException if the shared instance has not been configured.
         */
        @JvmStatic
        var sharedInstance: Purchases
            get() =
                backingFieldSharedInstance
                    ?: throw UninitializedPropertyAccessException(ConfigureStrings.NO_SINGLETON_INSTANCE)

            @VisibleForTesting(otherwise = VisibleForTesting.NONE)
            internal set(value) {
                backingFieldSharedInstance?.close()
                backingFieldSharedInstance = value
            }

        /**
         * Current version of the Purchases SDK
         */
        @JvmStatic
        val frameworkVersion = PurchasesOrchestrator.frameworkVersion

        /**
         * Set this property to your proxy URL before configuring Purchases *only*
         * if you've received a proxy key value from your RevenueCat contact.
         */
        @JvmStatic
        var proxyURL: URL?
            get() = PurchasesOrchestrator.proxyURL
            set(value) { PurchasesOrchestrator.proxyURL = value }

        /**
         * True if [configure] has been called and [Purchases.sharedInstance] is set
         */
        @JvmStatic
        val isConfigured: Boolean
            get() = this.backingFieldSharedInstance != null

        /**
         * Configures an instance of the Purchases SDK with a specified API key. The instance will
         * be set as a singleton. You should access the singleton instance using [Purchases.sharedInstance]
         * @param context: the Application context object of your Application.
         * @param apiKey: the API Key for your app. Obtained from the RevenueCat dashboard.
         * @param appUserID: a unique id for identifying the user.
         * @return An instantiated `[Purchases] object that has been set as a singleton.
         */
        @JvmStatic
        fun configureInCustomEntitlementsComputationMode(
            context: Context,
            apiKey: String,
            appUserID: String,
        ): Purchases {
            if (isConfigured) {
                infoLog(ConfigureStrings.INSTANCE_ALREADY_EXISTS)
            }
            val configuration = PurchasesConfiguration.Builder(context, apiKey)
                .appUserID(appUserID)
                .dangerousSettings(DangerousSettings(customEntitlementComputation = true))
                .pendingTransactionsForPrepaidPlansEnabled(true)
                .build()
            return PurchasesFactory().createPurchases(
                configuration,
                platformInfo,
                proxyURL,
            ).also {
                @SuppressLint("RestrictedApi")
                sharedInstance = it
            }
        }

        /**
         * Note: This method only works for the Google Play Store. There is no Amazon equivalent at this time.
         * Calling from an Amazon-configured app will return true.
         *
         * Check if billing is supported for the current Play user (meaning IN-APP purchases are supported)
         * and optionally, whether all features in the list of specified feature types are supported. This method is
         * asynchronous since it requires a connected BillingClient.
         * @param context A context object that will be used to connect to the billing client
         * @param features A list of feature types to check for support. Feature types must be one of [BillingFeature]
         *                 By default, is an empty list and no specific feature support will be checked.
         * @param callback Callback that will be notified when the check is complete.
         */
        @JvmStatic
        @JvmOverloads
        fun canMakePayments(
            context: Context,
            features: List<BillingFeature> = listOf(),
            callback: Callback<Boolean>,
        ) {
            val currentStore = sharedInstance.purchasesOrchestrator.appConfig.store
            if (currentStore != Store.PLAY_STORE) {
                log(LogIntent.RC_ERROR, BillingStrings.CANNOT_CALL_CAN_MAKE_PAYMENTS)
                callback.onReceived(true)
                return
            }
            PurchasesOrchestrator.canMakePayments(context, features, callback)
        }
    }

    // endregion
}
