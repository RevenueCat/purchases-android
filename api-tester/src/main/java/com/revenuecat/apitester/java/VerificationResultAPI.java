package com.revenuecat.apitester.java;

import com.revenuecat.purchases.VerificationResult;

@SuppressWarnings({"unused"})
final class VerificationResultAPI {
    static void check(final VerificationResult verificationResult) {
        switch (verificationResult) {
            case NOT_VERIFIED:
            case SUCCESS:
            case ERROR:
        }
    }
}
