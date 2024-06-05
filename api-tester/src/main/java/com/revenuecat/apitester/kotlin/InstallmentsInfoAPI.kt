package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.GoogleInstallmentsInfo
import com.revenuecat.purchases.models.InstallmentsInfo

@Suppress("unused", "UNUSED_VARIABLE")
private class InstallmentsInfoAPI {
    fun checkInstallmentsInfo(installmentsInfo: InstallmentsInfo) {
        val commitmentPaymentsCount: Int = installmentsInfo.commitmentPaymentsCount
        val subsequentCommitmentPaymentsCount: Int = installmentsInfo.subsequentCommitmentPaymentsCount
    }

    fun checkGoogleInstallmentsInfo(googleInstallmentsInfo: GoogleInstallmentsInfo) {
        checkInstallmentsInfo(googleInstallmentsInfo)
        val commitmentPaymentsCount: Int = googleInstallmentsInfo.commitmentPaymentsCount
        val subsequentCommitmentPaymentsCount: Int = googleInstallmentsInfo.subsequentCommitmentPaymentsCount

        val newGoogleInstallmentsInfo = GoogleInstallmentsInfo(
            commitmentPaymentsCount,
            subsequentCommitmentPaymentsCount,
        )
    }
}
