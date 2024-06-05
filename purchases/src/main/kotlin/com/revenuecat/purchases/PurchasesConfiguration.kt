package com.revenuecat.purchases

import android.content.Context
import java.util.concurrent.ExecutorService

/**
 * Holds parameters to initialize the SDK. Create an instance of this class using the [Builder] and pass it to
 * [Purchases.configure].
 */
open class PurchasesConfiguration(builder: Builder) {

    val context: Context
    val apiKey: String
    val appUserID: String?
    val observerMode: Boolean
    val showInAppMessagesAutomatically: Boolean
    val service: ExecutorService?
    val store: Store
    val diagnosticsEnabled: Boolean
    val dangerousSettings: DangerousSettings
    val verificationMode: EntitlementVerificationMode
    val pendingTransactionsForPrepaidPlansEnabled: Boolean

    init {
        this.context = builder.context
        this.apiKey = builder.apiKey
        this.appUserID = builder.appUserID
        this.observerMode = builder.observerMode
        this.service = builder.service
        this.store = builder.store
        this.diagnosticsEnabled = builder.diagnosticsEnabled
        this.verificationMode = builder.verificationMode
        this.dangerousSettings = builder.dangerousSettings
        this.showInAppMessagesAutomatically = builder.showInAppMessagesAutomatically
        this.pendingTransactionsForPrepaidPlansEnabled = builder.pendingTransactionsForPrepaidPlansEnabled
    }

    @Suppress("TooManyFunctions")
    open class Builder(
        @get:JvmSynthetic internal val context: Context,
        @get:JvmSynthetic internal val apiKey: String,
    ) {

        @set:JvmSynthetic @get:JvmSynthetic
        internal var appUserID: String? = null

        @set:JvmSynthetic @get:JvmSynthetic
        internal var observerMode: Boolean = false

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
        fun observerMode(observerMode: Boolean) = apply {
            this.observerMode = observerMode
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
         * This information will be anonymized so it can't be traced back to the end-user.
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
}
