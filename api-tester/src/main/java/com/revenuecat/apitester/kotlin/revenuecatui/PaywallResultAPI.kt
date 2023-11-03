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

    fun checkResult(error: PurchasesError, customerInfo: CustomerInfo) {
        val result: PaywallResult = PaywallResult.Cancelled
        val parcelable: Parcelable = result
        val result2 = PaywallResult.Error(error)
        val error2: PurchasesError = result2.error
        val result3 = PaywallResult.Purchased(customerInfo)
        val customerInfo: CustomerInfo = result3.customerInfo
        val result2AsSealedClass: PaywallResult = result2
        val result3AsSealedClass: PaywallResult = result3
    }
}
