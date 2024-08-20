package com.revenuecat.apitester.kotlin.revenuecatui

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicWithCallback
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicPurchaseResult
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicRestoreResult

@Suppress("unused", "UNUSED_VARIABLE")
private class PurchaseLogicAPI {

    suspend fun check(
        mySuspendLogic: PurchaseLogic,
        activity: Activity,
        rcPackage: Package,
        customerInfo: CustomerInfo,
    ) {
        val suspendLogicPurchase: PurchaseLogicPurchaseResult = mySuspendLogic.performPurchase(activity, rcPackage)
        val suspendLogicRestore: PurchaseLogicRestoreResult = mySuspendLogic.performRestore(customerInfo)
    }
}

@Suppress("unused")
private class PurchaseLogicWithCallbackAPI : PurchaseLogicWithCallback() {

    override fun performPurchaseWithCompletion(
        activity: Activity,
        rcPackage: Package,
        completion: (PurchaseLogicPurchaseResult) -> Unit,
    ) {
        val success = PurchaseLogicPurchaseResult.Success
        val cancelled = PurchaseLogicPurchaseResult.Cancellation
        val failed = PurchaseLogicPurchaseResult.Error(PurchasesError(PurchasesErrorCode.StoreProblemError))
        completion(success)
    }

    override fun performRestoreWithCompletion(customerInfo: CustomerInfo, completion: (PurchaseLogicRestoreResult) -> Unit) {
        val success = PurchaseLogicRestoreResult.Success
        val failed = PurchaseLogicRestoreResult.Error(PurchasesError(PurchasesErrorCode.StoreProblemError))
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
        ) { result: PurchaseLogicPurchaseResult -> }

        performRestoreWithCompletion(
            customerInfo,
        ) { result: PurchaseLogicRestoreResult -> }
    }
}
