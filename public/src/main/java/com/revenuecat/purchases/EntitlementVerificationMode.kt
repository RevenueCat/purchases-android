package com.revenuecat.purchases

/**
 * Verification strictness levels for [EntitlementInfo].
 */
enum class EntitlementVerificationMode {
    /**
     * The SDK will not perform any entitlement verification.
     */
    DISABLED,

    /**
     * Enable entitlement verification.
     *
     * If verification fails, this will be indicated with [VerificationResult.FAILED]
     * but parsing will not fail.
     *
     * This can be useful if you want to handle validation failures but still grant access.
     */
    INFORMATIONAL,

    /**
     * Enable entitlement verification.
     *
     * If verification fails when fetching [CustomerInfo] and/or [EntitlementInfos]
     * the request will fail.
     */
    ENFORCED;

    companion object {
        /**
         * Default entitlement verification mode.
         */
        val default: EntitlementVerificationMode
            get() = DISABLED
    }

    val shouldVerify: Boolean
        get() = when (this) {
            DISABLED -> false
            INFORMATIONAL, ENFORCED -> true
        }
}
