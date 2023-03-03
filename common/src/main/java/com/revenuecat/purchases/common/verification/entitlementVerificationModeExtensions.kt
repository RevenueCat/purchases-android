package com.revenuecat.purchases.common.verification

import com.revenuecat.purchases.EntitlementVerificationMode

val EntitlementVerificationMode.shouldVerify: Boolean
    get() = when (this) {
        EntitlementVerificationMode.DISABLED -> false
        EntitlementVerificationMode.INFORMATIONAL, EntitlementVerificationMode.ENFORCED -> true
    }
