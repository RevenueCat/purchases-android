package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.GoogleInstallmentsInfo;
import com.revenuecat.purchases.models.InstallmentsInfo;

@SuppressWarnings({"unused"})
final class InstallmentsInfoAPI {
    static void check(final InstallmentsInfo installmentsInfo) {
        int commitmentPaymentsCount = installmentsInfo.getCommitmentPaymentsCount();
        int subsequentCommitmentPaymentsCount = installmentsInfo.getSubsequentCommitmentPaymentsCount();
    }

    static void checkGoogleInstallmentsInfo(final GoogleInstallmentsInfo googleInstallmentsInfo) {
        check(googleInstallmentsInfo);
        int commitmentPaymentsCount = googleInstallmentsInfo.getCommitmentPaymentsCount();
        int subsequentCommitmentPaymentsCount = googleInstallmentsInfo.getSubsequentCommitmentPaymentsCount();

        GoogleInstallmentsInfo constructedGoogleInstallmentsInfo = new GoogleInstallmentsInfo(
                commitmentPaymentsCount,
                subsequentCommitmentPaymentsCount
        );
    }
}
