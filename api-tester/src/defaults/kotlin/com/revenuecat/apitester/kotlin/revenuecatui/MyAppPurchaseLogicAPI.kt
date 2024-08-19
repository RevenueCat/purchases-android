package com.revenuecat.apitester.kotlin.revenuecatui

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ui.revenuecatui.MyAppPurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.MyAppPurchaseLogicWithCallback
import com.revenuecat.purchases.ui.revenuecatui.MyAppPurchaseResult
import com.revenuecat.purchases.ui.revenuecatui.MyAppRestoreResult

@Suppress("unused", "UNUSED_VARIABLE")
private class MyAppPurchaseLogicAPI {

    suspend fun check(
        mySuspendLogic: MyAppPurchaseLogic,
        activity: Activity,
        rcPackage: Package,
        customerInfo: CustomerInfo,
    ) {
        val suspendLogicPurchase: MyAppPurchaseResult = mySuspendLogic.performPurchase(activity, rcPackage)
        val suspendLogicRestore: MyAppRestoreResult = mySuspendLogic.performRestore(customerInfo)
    }
}

@Suppress("unused")
private class MyAppPurchaseLogicWithCallbackAPI : MyAppPurchaseLogicWithCallback() {

    override fun performPurchaseWithCompletion(
        activity: Activity,
        rcPackage: Package,
        completion: (MyAppPurchaseResult) -> Unit,
    ) {
        val success = MyAppPurchaseResult.Success
        val cancelled = MyAppPurchaseResult.Cancellation
        val failed = MyAppPurchaseResult.Error(PurchasesError(PurchasesErrorCode.StoreProblemError))
        completion(success)
    }

    override fun performRestoreWithCompletion(customerInfo: CustomerInfo, completion: (MyAppRestoreResult) -> Unit) {
        val success = MyAppRestoreResult.Success
        val failed = MyAppRestoreResult.Error(PurchasesError(PurchasesErrorCode.StoreProblemError))
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
        ) { result: MyAppPurchaseResult -> }

        performRestoreWithCompletion(
            customerInfo,
        ) { result: MyAppRestoreResult -> }
    }
}
