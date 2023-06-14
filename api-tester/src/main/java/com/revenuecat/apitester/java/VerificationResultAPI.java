package com.revenuecat.apitester.java;

import com.revenuecat.purchases.VerificationResult;

@SuppressWarnings({"unused"})
final class VerificationResultAPI {
    static void check(final VerificationResult verificationResult) {
        switch (verificationResult) {
            case VERIFIED_ON_DEVICE:
            case NOT_REQUESTED:
            case VERIFIED:
            case FAILED:
        }
    }
}
