package com.revenuecat.purchases

import android.content.Context
import java.util.concurrent.ExecutorService

open class PurchasesConfiguration(builder: Builder) {

    val context: Context
    val apiKey: String
    val appUserID: String?
    val observerMode: Boolean
    val service: ExecutorService?
    val store: Store
    val diagnosticsEnabled: Boolean
    val dangerousSettings: DangerousSettings
    val verificationMode: EntitlementVerificationMode

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
    }

    open class Builder(
        @get:JvmSynthetic internal val context: Context,
        @get:JvmSynthetic internal val apiKey: String,
    ) {

        @set:JvmSynthetic @get:JvmSynthetic
        internal var appUserID: String? = null

        @set:JvmSynthetic @get:JvmSynthetic
        internal var observerMode: Boolean = false

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

        fun appUserID(appUserID: String?) = apply {
            this.appUserID = appUserID
        }

        fun observerMode(observerMode: Boolean) = apply {
            this.observerMode = observerMode
        }

        fun service(service: ExecutorService) = apply {
            this.service = service
        }

        fun store(store: Store) = apply {
            this.store = store
        }

        /**
         * Enabling diagnostics will send some performance and debugging information from the SDK to our servers.
         * Examples of this information include response times, cache hits or error codes.
         * This information will be anonymized so it can't be traced back to the end-user.
         * The default value is false.
         */
        fun diagnosticsEnabled(diagnosticsEnabled: Boolean) = apply {
            this.diagnosticsEnabled = diagnosticsEnabled
            if (!diagnosticsEnabled) {
                this.verificationMode = EntitlementVerificationMode.DISABLED
            }
        }

        /**
         * **Feature in beta**. Enables signature verification of requests to the RevenueCat backend
         * in informational mode and enables diagnostics reports to RevenueCat to help us analyze this feature.
         * Informational mode means that the behavior will remain the same but we will provide information about
         * detected attempts of hacking.
         *
         * When changing from disabled to enabled, the SDK will clear the CustomerInfo cache.
         * This means users will need to connect to the internet to get back their entitlements.
         *
         * The result of the verification can be obtained from [EntitlementInfos.verification] or
         * [EntitlementInfo.verification].
         *
         * This feature is currently in beta and the behavior may change.
         *
         * Default mode is disabled.
         */
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
         * Only use a Dangerous Setting if suggested by RevenueCat support team.
         */
        fun dangerousSettings(dangerousSettings: DangerousSettings) = apply {
            this.dangerousSettings = dangerousSettings
        }

        open fun build(): PurchasesConfiguration {
            return PurchasesConfiguration(this)
        }
    }
}
