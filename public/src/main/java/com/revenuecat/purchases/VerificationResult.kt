package com.revenuecat.purchases

/**
 * The result of the verification process.
 *
 * This is accomplished by preventing MiTM attacks between the SDK and the RevenueCat server.
 * With verification enabled, the SDK ensures that the response created by the server was not
 * modified by a third-party, and the entitlements received are exactly what was sent.
 *
 * - Note: Entitlements are only verified if enabled using PurchasesConfiguration.Builder's
 * entitlementVerificationMode property. This is disabled by default.
 */
enum class VerificationResult {
    /**
     * No verification was done.
     *
     * This can happen for multiple reasons:
     * 1. Verification is not enabled in PurchasesConfiguration
     * 2. Data was cached in an older version of the SDK not supporting verification
     */
    NOT_VERIFIED,

    /**
     * Entitlements were verified with our server.
     */
    SUCCESS,

    /**
     * Entitlement verification failed, possibly due to a MiTM attack.
     */
    ERROR
}
