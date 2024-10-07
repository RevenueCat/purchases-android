package com.revenuecat.purchases

import android.content.Context
import com.revenuecat.purchases.PurchasesConfiguration.Builder
import java.util.concurrent.ExecutorService

/**
 * Holds parameters to initialize the SDK. Create an instance of this class using the [Builder] and pass it to
 * [Purchases.configure].
 */
open class PurchasesConfiguration(builder: Builder) {

    val context: Context
    val apiKey: String
    val appUserID: String?

    @Deprecated(
        "observerMode is being deprecated in favor of purchasesAreCompletedBy.",
        ReplaceWith(
            "purchasesAreCompletedBy == MY_APP",
            "com.revenuecat.purchases.PurchasesAreCompletedBy.MY_APP",
        ),
    )
    val observerMode: Boolean
        get() = when (purchasesAreCompletedBy) {
            PurchasesAreCompletedBy.REVENUECAT -> false
            PurchasesAreCompletedBy.MY_APP -> true
        }
    val purchasesAreCompletedBy: PurchasesAreCompletedBy
    val showInAppMessagesAutomatically: Boolean
    val service: ExecutorService?
    val store: Store
    val diagnosticsEnabled: Boolean
    val dangerousSettings: DangerousSettings
    val verificationMode: EntitlementVerificationMode
    val pendingTransactionsForPrepaidPlansEnabled: Boolean

    init {
        this.context = builder.context.applicationContext
        this.apiKey = builder.apiKey.trim()
        this.appUserID = builder.appUserID
        this.purchasesAreCompletedBy = builder.purchasesAreCompletedBy
        this.service = builder.service
        this.store = builder.store
        this.diagnosticsEnabled = builder.diagnosticsEnabled
        this.verificationMode = builder.verificationMode
        this.dangerousSettings = builder.dangerousSettings
        this.showInAppMessagesAutomatically = builder.showInAppMessagesAutomatically
        this.pendingTransactionsForPrepaidPlansEnabled = builder.pendingTransactionsForPrepaidPlansEnabled
    }

    @SuppressWarnings("TooManyFunctions")
    open class Builder(
        @get:JvmSynthetic internal val context: Context,
        @get:JvmSynthetic internal val apiKey: String,
    ) {

        @set:JvmSynthetic @get:JvmSynthetic
        internal var appUserID: String? = null

        @set:JvmSynthetic @get:JvmSynthetic
        internal var purchasesAreCompletedBy: PurchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT

        @set:JvmSynthetic @get:JvmSynthetic
        internal var showInAppMessagesAutomatically: Boolean = true

        @set:JvmSynthetic @get:JvmSynthetic
        internal var service: ExecutorService? = null

        @set:JvmSynthetic @get:JvmSynthetic
        internal var store: Store = Store.PLAY_STORE

        @set:JvmSynthetic @get:JvmSynthetic
        internal var diagnosticsEnabled: Boolean = false

        @set:JvmSynthetic @get:JvmSynthetic
        internal var verificationMode: EntitlementVerificationMode = EntitlementVerificationMode.default

        @set:JvmSynthetic @get:JvmSynthetic
        internal var dangerousSettings: DangerousSettings = DangerousSettings()

        @set:JvmSynthetic @get:JvmSynthetic
        internal var pendingTransactionsForPrepaidPlansEnabled: Boolean = false

        /**
         * A unique id for identifying the user
         */
        fun appUserID(appUserID: String?) = apply {
            this.appUserID = appUserID
        }

        /**
         * Enable this setting to show in-app messages from Google Play automatically. Default is enabled.
         * For more info: https://rev.cat/googleplayinappmessaging
         *
         * If this setting is disabled, you can show the snackbar by calling
         * [Purchases.showInAppMessagesIfNeeded]
         */
        fun showInAppMessagesAutomatically(showInAppMessagesAutomatically: Boolean) = apply {
            this.showInAppMessagesAutomatically = showInAppMessagesAutomatically
        }

        /**
         * An optional boolean. Set this to TRUE if you have your own IAP implementation and
         * want to use only RevenueCat's backend. Default is FALSE. If you are on Android and setting this to TRUE,
         * you will have to acknowledge the purchases yourself.
         */
        @Deprecated(
            "observerMode() is being deprecated in favor of purchasesAreCompletedBy().",
            ReplaceWith(
                "purchasesAreCompletedBy(if (observerMode) MY_APP else REVENUECAT)",
                "com.revenuecat.purchases.PurchasesAreCompletedBy.REVENUECAT",
                "com.revenuecat.purchases.PurchasesAreCompletedBy.MY_APP",
            ),
        )
        fun observerMode(observerMode: Boolean) = apply {
            purchasesAreCompletedBy(
                if (observerMode) {
                    PurchasesAreCompletedBy.MY_APP
                } else {
                    PurchasesAreCompletedBy.REVENUECAT
                },
            )
        }

        /**
         * An optional setting. Set this to [MY_APP][PurchasesAreCompletedBy.MY_APP] if you have your own IAP
         * implementation and want to use only RevenueCat's backend. Default is
         * [REVENUECAT][PurchasesAreCompletedBy.REVENUECAT]. If you are on Android and setting this to
         * [MY_APP][PurchasesAreCompletedBy.MY_APP], you will have to acknowledge the purchases yourself.
         *
         * **Note:** failing to acknowledge a purchase within 3 days will lead to Google Play automatically issuing a
         * refund to the user.
         *
         * For more info, see
         * [revenuecat.com](https://www.revenuecat.com/docs/migrating-to-revenuecat/sdk-or-not/finishing-transactions)
         * and [developer.android.com](https://developer.android.com/google/play/billing/integrate#process).
         */
        fun purchasesAreCompletedBy(purchasesAreCompletedBy: PurchasesAreCompletedBy) = apply {
            this.purchasesAreCompletedBy = purchasesAreCompletedBy
        }

        /**
         * Executor service for performing backend operations. This can be used if you want to share an executor between
         * Purchases and your own code. If not passed in, one will be created.
         */
        fun service(service: ExecutorService) = apply {
            this.service = service
        }

        /**
         * The store in which to make purchases. See [Store] for supported stores.
         * @see Store
         */
        fun store(store: Store) = apply {
            this.store = store
        }

        /**
         * Enabling diagnostics will send some performance and debugging information from the SDK to our servers.
         * Examples of this information include response times, cache hits or error codes.
         * No personal identifiable information will be collected.
         * The default value is false.
         *
         * Diagnostics is only available in Android API 24+
         */
        fun diagnosticsEnabled(diagnosticsEnabled: Boolean) = apply {
            this.diagnosticsEnabled = diagnosticsEnabled
        }

        /**
         * Deprecated. Use [entitlementVerificationMode] instead.
         *
         * Enables signature verification of requests to the RevenueCat backend
         * and enables diagnostics reports to RevenueCat to help us analyze this feature.
         *
         * When changing from disabled to enabled, the SDK will clear the CustomerInfo cache.
         * This means users will need to connect to the internet to get their entitlements back.
         *
         * The result of the verification can be obtained from [EntitlementInfos.verification] or
         * [EntitlementInfo.verification].
         *
         * Default mode is disabled.
         *
         * @warning This function is marked as [ExperimentalPreviewRevenueCatPurchasesAPI] and may change in the future.
         */
        @Deprecated(
            "Use the new entitlementVerificationMode setter instead.",
            ReplaceWith("entitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)"),
        )
        @JvmSynthetic
        @ExperimentalPreviewRevenueCatPurchasesAPI
        fun informationalVerificationModeAndDiagnosticsEnabled(enabled: Boolean) = apply {
            if (enabled) {
                this.verificationMode = EntitlementVerificationMode.INFORMATIONAL
                this.diagnosticsEnabled = true
            } else {
                this.verificationMode = EntitlementVerificationMode.DISABLED
                this.diagnosticsEnabled = false
            }
        }

        /**
         * Sets the [EntitlementVerificationMode] to perform signature verification of requests to the
         * RevenueCat backend.
         *
         * When changing from [EntitlementVerificationMode.DISABLED] to other modes, the SDK will clear the
         * CustomerInfo cache.
         * This means users will need to connect to the internet to get their entitlements back.
         *
         * The result of the verification can be obtained from [EntitlementInfos.verification] or
         * [EntitlementInfo.verification].
         *
         * Default mode is disabled. Please see https://rev.cat/trusted-entitlements for more info.
         */
        fun entitlementVerificationMode(verificationMode: EntitlementVerificationMode) = apply {
            this.verificationMode = verificationMode
        }

        /**
         * Only use a Dangerous Setting if suggested by RevenueCat support team.
         */
        fun dangerousSettings(dangerousSettings: DangerousSettings) = apply {
            this.dangerousSettings = dangerousSettings
        }

        /**
         * Enable this setting if you want to allow pending purchases for prepaid subscriptions (only supported
         * in Google Play). Note that entitlements are not granted until payment is done.
         * Default is disabled.
         */
        fun pendingTransactionsForPrepaidPlansEnabled(pendingTransactionsForPrepaidPlansEnabled: Boolean) = apply {
            this.pendingTransactionsForPrepaidPlansEnabled = pendingTransactionsForPrepaidPlansEnabled
        }

        /**
         * Creates a [PurchasesConfiguration] instance with the specified properties.
         */
        open fun build(): PurchasesConfiguration {
            return PurchasesConfiguration(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PurchasesConfiguration

        if (context != other.context) return false
        if (apiKey != other.apiKey) return false
        if (appUserID != other.appUserID) return false
        if (purchasesAreCompletedBy != other.purchasesAreCompletedBy) return false
        if (showInAppMessagesAutomatically != other.showInAppMessagesAutomatically) return false
        if (service != other.service) return false
        if (store != other.store) return false
        if (diagnosticsEnabled != other.diagnosticsEnabled) return false
        if (dangerousSettings != other.dangerousSettings) return false
        if (verificationMode != other.verificationMode) return false
        if (pendingTransactionsForPrepaidPlansEnabled != other.pendingTransactionsForPrepaidPlansEnabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + apiKey.hashCode()
        result = 31 * result + (appUserID?.hashCode() ?: 0)
        result = 31 * result + purchasesAreCompletedBy.hashCode()
        result = 31 * result + showInAppMessagesAutomatically.hashCode()
        result = 31 * result + (service?.hashCode() ?: 0)
        result = 31 * result + store.hashCode()
        result = 31 * result + diagnosticsEnabled.hashCode()
        result = 31 * result + dangerousSettings.hashCode()
        result = 31 * result + verificationMode.hashCode()
        result = 31 * result + pendingTransactionsForPrepaidPlansEnabled.hashCode()
        return result
    }
}
