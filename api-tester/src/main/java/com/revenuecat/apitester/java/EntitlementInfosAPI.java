package com.revenuecat.apitester.java;

import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.EntitlementInfos;
import com.revenuecat.purchases.VerificationResult;

import java.util.Map;

@SuppressWarnings({"unused", "deprecation"})
final class EntitlementInfosAPI {
    static void check(final EntitlementInfos infos) {
        final Map<String, EntitlementInfo> active = infos.getActive();
        final Map<String, EntitlementInfo> all = infos.getAll();
        final EntitlementInfo i = infos.get("");
        final VerificationResult verification = infos.getVerification();
    }

    static void checkConstructor(
            Map<String, EntitlementInfo> all,
            VerificationResult verificationResult
    ) {
        final EntitlementInfos entitlementInfos = new EntitlementInfos(
                all,
                verificationResult
        );
        final EntitlementInfos entitlementInfos2 = new EntitlementInfos(all);
    }
}
