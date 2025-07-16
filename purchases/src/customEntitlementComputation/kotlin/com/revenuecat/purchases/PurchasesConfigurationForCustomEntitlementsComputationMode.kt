package com.revenuecat.purchases

import android.content.Context
import com.revenuecat.purchases.PurchasesConfigurationForCustomEntitlementsComputationMode.Builder

/**
 * Holds parameters to initialize the SDK in Custom Entitlements Computation mode. Create an instance of this class
 * using the [Builder] and pass it to [Purchases.configureInCustomEntitlementsComputationMode].
 */
public class PurchasesConfigurationForCustomEntitlementsComputationMode internal constructor(
    internal val context: Context,
    internal val apiKey: String,
    internal val appUserID: String,
    internal val showInAppMessagesAutomatically: Boolean,
    internal val pendingTransactionsForPrepaidPlansEnabled: Boolean,
) {
    /**
     * @param context: the Application context object of your Application.
     * @param apiKey: the API Key for your app. Obtained from the RevenueCat dashboard.
     * @param appUserID: a unique id for identifying the user.
     */
    public class Builder(
        private val context: Context,
        private val apiKey: String,
        private val appUserID: String,
    ) {
        private var showInAppMessagesAutomatically: Boolean = true
        private var pendingTransactionsForPrepaidPlansEnabled: Boolean = true

        /**
         * Enable this setting to show in-app messages from Google Play automatically. Default is enabled.
         * For more info: [rev.cat](https://rev.cat/googleplayinappmessaging).
         *
         * If this setting is disabled, you can show the snackbar by calling [Purchases.showInAppMessagesIfNeeded].
         */
        public fun showInAppMessagesAutomatically(enabled: Boolean): Builder {
            showInAppMessagesAutomatically = enabled
            return this
        }

        /**
         * Enable this setting if you want to allow pending purchases for prepaid subscriptions (only supported in
         * Google Play). Note that entitlements are not granted until payment is done. Default is enabled.
         */
        public fun pendingTransactionsForPrepaidPlansEnabled(enabled: Boolean): Builder {
            pendingTransactionsForPrepaidPlansEnabled = enabled
            return this
        }

        /**
         * Creates a [PurchasesConfigurationForCustomEntitlementsComputationMode] instance with the specified
         * properties.
         */
        public fun build(): PurchasesConfigurationForCustomEntitlementsComputationMode =
            PurchasesConfigurationForCustomEntitlementsComputationMode(
                context = context,
                apiKey = apiKey,
                appUserID = appUserID,
                showInAppMessagesAutomatically = showInAppMessagesAutomatically,
                pendingTransactionsForPrepaidPlansEnabled = pendingTransactionsForPrepaidPlansEnabled,
            )
    }
}
