package com.revenuecat.purchases

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Only use a Dangerous Setting if suggested by RevenueCat support team.
 */
@Parcelize
@Poko
public class DangerousSettings internal constructor(
    /**
     * Disable or enable syncing purchases automatically. If this is disabled, RevenueCat will not sync any purchase
     * automatically, and you will have to call syncPurchases whenever a new purchase is completed in order to send it
     * to the RevenueCat's backend. Auto syncing of purchases is enabled by default.
     */
    public val autoSyncPurchases: Boolean = true,

    internal val customEntitlementComputation: Boolean = false,

    internal val uiPreviewMode: Boolean = false,

    internal val applyObfuscatedAccountIdToSubscriptionChanges: Boolean = false,

    /**
     * Enables RevenueCat Workflows (multipage paywalls). Internal RevenueCat use only.
     */
    @InternalRevenueCatAPI
    public val useWorkflows: Boolean = false,
) : Parcelable {
    @OptIn(InternalRevenueCatAPI::class)
    public constructor(autoSyncPurchases: Boolean = true) : this(
        autoSyncPurchases = autoSyncPurchases,
        customEntitlementComputation = false,
        uiPreviewMode = false,
        applyObfuscatedAccountIdToSubscriptionChanges = false,
        useWorkflows = false,
    )

    public companion object {
        /**
         * Creates a [DangerousSettings] configured for UI preview mode. When enabled, the SDK
         * bypasses billing, identity creation, and other subsystems to function purely as a
         * paywall rendering engine. Auto sync of purchases is forced off in this mode.
         */
        @InternalRevenueCatAPI
        @JvmStatic
        public fun forPreviewMode(): DangerousSettings = DangerousSettings(
            autoSyncPurchases = false,
            customEntitlementComputation = false,
            uiPreviewMode = true,
            applyObfuscatedAccountIdToSubscriptionChanges = false,
        )

        /**
         * Creates a [DangerousSettings] with RevenueCat Workflows (multipage paywalls) enabled.
         * Internal RevenueCat use only; behavior may change without warning.
         */
        @InternalRevenueCatAPI
        @JvmStatic
        public fun forWorkflows(autoSyncPurchases: Boolean = true): DangerousSettings = DangerousSettings(
            autoSyncPurchases = autoSyncPurchases,
            customEntitlementComputation = false,
            uiPreviewMode = false,
            applyObfuscatedAccountIdToSubscriptionChanges = false,
            useWorkflows = true,
        )
    }
}
