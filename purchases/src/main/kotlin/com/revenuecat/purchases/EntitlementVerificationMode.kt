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
     * If verification fails, this will be indicated with [VerificationResult.FAILED] in
     * the [EntitlementInfos.verification] and [EntitlementInfo.verification] properties but parsing will not fail
     * (i.e. Entitlements will still be granted).
     *
     * This can be useful if you want to handle verification failures to display an error/warning to the user
     * or to track this situation but still grant access.
     */
    INFORMATIONAL,

    ;

    // Hidden ENFORCED mode during feature beta
    // ENFORCED;

    companion object {
        /**
         * Default entitlement verification mode.
         */
        val default: EntitlementVerificationMode
            get() = DISABLED
    }

    val isEnabled: Boolean
        get() = this != DISABLED
}
