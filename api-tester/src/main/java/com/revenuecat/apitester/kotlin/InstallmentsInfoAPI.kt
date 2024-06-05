package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.GoogleInstallmentsInfo
import com.revenuecat.purchases.models.InstallmentsInfo

@Suppress("unused", "UNUSED_VARIABLE")
private class InstallmentsInfoAPI {
    fun checkInstallmentsInfo(installmentsInfo: InstallmentsInfo) {
        val commitmentPaymentsCount: Int = installmentsInfo.commitmentPaymentsCount
        val renewalCommitmentPaymentsCount: Int = installmentsInfo.renewalCommitmentPaymentsCount
    }

    fun checkGoogleInstallmentsInfo(googleInstallmentsInfo: GoogleInstallmentsInfo) {
        checkInstallmentsInfo(googleInstallmentsInfo)
        val commitmentPaymentsCount: Int = googleInstallmentsInfo.commitmentPaymentsCount
        val renewalCommitmentPaymentsCount: Int = googleInstallmentsInfo.renewalCommitmentPaymentsCount

        val newGoogleInstallmentsInfo = GoogleInstallmentsInfo(
            commitmentPaymentsCount,
            renewalCommitmentPaymentsCount,
        )
    }
}
