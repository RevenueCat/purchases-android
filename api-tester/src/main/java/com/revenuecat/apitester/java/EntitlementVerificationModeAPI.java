package com.revenuecat.apitester.java;

import com.revenuecat.purchases.EntitlementVerificationMode;

@SuppressWarnings({"unused"})
final class EntitlementVerificationModeAPI {
    static void check(final EntitlementVerificationMode verificationMode) {
        switch (verificationMode) {
            case DISABLED:
            case INFORMATIONAL:
            // Hidden ENFORCED mode during feature beta
            // case ENFORCED:
        }
    }
}
