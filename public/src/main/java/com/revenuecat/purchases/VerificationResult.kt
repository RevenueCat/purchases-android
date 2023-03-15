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
     * This value is returned when verification is not enabled in PurchasesConfiguration
     */
    NOT_REQUESTED,

    /**
     * Entitlements were verified with our server.
     */
    VERIFIED,

    /**
     * Entitlement verification failed, possibly due to a MiTM attack.
     */
    FAILED
}
