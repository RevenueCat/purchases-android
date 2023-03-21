package com.revenuecat.apitester.java;

import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.EntitlementInfos;

import java.util.Map;

@SuppressWarnings({"unused", "deprecation"})
final class EntitlementInfosAPI {
    static void check(final EntitlementInfos infos) {
        final Map<String, EntitlementInfo> active = infos.getActive();
        final Map<String, EntitlementInfo> all = infos.getAll();
        final EntitlementInfo i = infos.get("");
        // Trusted entitlements: Commented out until ready to be made public
        // final VerificationResult verification = infos.getVerification();
    }

    static void checkConstructor(
            Map<String, EntitlementInfo> all
            // Trusted entitlements: Commented out until ready to be made public
            // VerificationResult verificationResult
    ) {
        final EntitlementInfos entitlementInfos = new EntitlementInfos(
                all
                // Trusted entitlements: Commented out until ready to be made public
                // verificationResult
        );
        final EntitlementInfos entitlementInfos2 = new EntitlementInfos(all);
    }
}
