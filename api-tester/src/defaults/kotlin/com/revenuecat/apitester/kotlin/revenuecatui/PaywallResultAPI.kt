package com.revenuecat.apitester.kotlin.revenuecatui

import android.os.Parcelable
import androidx.activity.result.ActivityResultCallback
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler

@Suppress("unused", "UNUSED_VARIABLE")
@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
private class PaywallResultAPI {
    fun checkResultHandler(
        resultHandler: PaywallResultHandler,
    ) {
        val callback: ActivityResultCallback<PaywallResult> = resultHandler
    }

    fun checkResult(paywallResult: PaywallResult, error: PurchasesError, customerInfo: CustomerInfo) {
        when (paywallResult) {
            is PaywallResult.Cancelled -> {}
            is PaywallResult.Error -> {
                val error2: PurchasesError = paywallResult.error
            }
            is PaywallResult.Purchased -> {
                val customerInfo2: CustomerInfo = paywallResult.customerInfo
            }
            is PaywallResult.Restored -> {
                val customerInfo3: CustomerInfo = paywallResult.customerInfo
            }
        }
        val parcelable: Parcelable = paywallResult
        val result2 = PaywallResult.Error(error)
        val result3 = PaywallResult.Purchased(customerInfo)
        val result4 = PaywallResult.Restored(customerInfo)
    }
}
