package com.revenuecat.apitester.kotlin.revenuecatui

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicWithCallback
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult

@Suppress("unused", "UNUSED_VARIABLE")
private class PurchaseLogicAPI {

    suspend fun check(
        mySuspendLogic: PurchaseLogic,
        activity: Activity,
        rcPackage: Package,
        customerInfo: CustomerInfo,
    ) {
        val suspendLogicPurchase: PurchaseLogicResult = mySuspendLogic.performPurchase(activity, rcPackage)
        val suspendLogicRestore: PurchaseLogicResult = mySuspendLogic.performRestore(customerInfo)
    }
}

@Suppress("unused")
private class PurchaseLogicWithCallbackAPI : PurchaseLogicWithCallback() {

    override fun performPurchaseWithCompletion(
        activity: Activity,
        rcPackage: Package,
        completion: (PurchaseLogicResult) -> Unit,
    ) {
        val success = PurchaseLogicResult.Success
        val cancelled = PurchaseLogicResult.Cancellation
        val failed = PurchaseLogicResult.Error(PurchasesError(PurchasesErrorCode.StoreProblemError))
        completion(success)
    }

    override fun performRestoreWithCompletion(customerInfo: CustomerInfo, completion: (PurchaseLogicResult) -> Unit) {
        val success = PurchaseLogicResult.Success
        val cancelled = PurchaseLogicResult.Cancellation
        val failed = PurchaseLogicResult.Error(PurchasesError(PurchasesErrorCode.StoreProblemError))
        completion(failed)
    }

    @Suppress("unused")
    fun check(
        activity: Activity,
        rcPackage: Package,
        customerInfo: CustomerInfo,
    ) {
        performPurchaseWithCompletion(
            activity,
            rcPackage,
        ) { result: PurchaseLogicResult -> }

        performRestoreWithCompletion(
            customerInfo,
        ) { result: PurchaseLogicResult -> }
    }
}
