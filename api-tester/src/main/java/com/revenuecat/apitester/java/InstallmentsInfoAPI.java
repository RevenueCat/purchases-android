package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.GoogleInstallmentsInfo;
import com.revenuecat.purchases.models.InstallmentsInfo;

@SuppressWarnings({"unused"})
final class InstallmentsInfoAPI {
    static void check(final InstallmentsInfo installmentsInfo) {
        int commitmentPaymentsCount = installmentsInfo.getCommitmentPaymentsCount();
        int renewalCommitmentPaymentsCount = installmentsInfo.getRenewalCommitmentPaymentsCount();
    }

    static void checkGoogleInstallmentsInfo(final GoogleInstallmentsInfo googleInstallmentsInfo) {
        check(googleInstallmentsInfo);
        int commitmentPaymentsCount = googleInstallmentsInfo.getCommitmentPaymentsCount();
        int renewalCommitmentPaymentsCount = googleInstallmentsInfo.getRenewalCommitmentPaymentsCount();

        GoogleInstallmentsInfo constructedGoogleInstallmentsInfo = new GoogleInstallmentsInfo(
                commitmentPaymentsCount,
                renewalCommitmentPaymentsCount
        );
    }
}
