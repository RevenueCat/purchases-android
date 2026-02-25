package com.revenuecat.apitester.kotlin.revenuecatui

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ReplacementMode
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogicParams
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogicWithCallback
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicWithCallback

@Suppress("unused", "UNUSED_VARIABLE")
private class PaywallPurchaseLogicAPI {

    suspend fun checkPurchase(
        logic: PaywallPurchaseLogic,
        activity: Activity,
        rcPackage: Package,
    ) {
        val params = PaywallPurchaseLogicParams.Builder(rcPackage).build()
        val result: PurchaseLogicResult = logic.performPurchase(activity, params)
    }

    suspend fun checkRestore(logic: PaywallPurchaseLogic, customerInfo: CustomerInfo) {
        val result: PurchaseLogicResult = logic.performRestore(customerInfo)
    }

    fun checkPaywallPurchaseLogicParams(rcPackage: Package, subscriptionOption: SubscriptionOption) {
        val params = PaywallPurchaseLogicParams.Builder(rcPackage)
            .oldProductId("old_product_id")
            .replacementMode(GoogleReplacementMode.DEFERRED)
            .subscriptionOption(subscriptionOption)
            .build()
        val pkg: Package = params.rcPackage
        val oldProductId: String? = params.oldProductId
        val replacementMode: ReplacementMode? = params.replacementMode
        val option: SubscriptionOption? = params.subscriptionOption
    }
}

@Suppress("unused")
private class PaywallPurchaseLogicSuspendAPI : PaywallPurchaseLogic {

    override suspend fun performPurchase(
        activity: Activity,
        params: PaywallPurchaseLogicParams,
    ): PurchaseLogicResult {
        return PurchaseLogicResult.Success
    }

    override suspend fun performRestore(customerInfo: CustomerInfo): PurchaseLogicResult {
        return PurchaseLogicResult.Success
    }
}

@Suppress("unused")
private class PaywallPurchaseLogicCallbackAPI : PaywallPurchaseLogicWithCallback() {

    override fun performPurchaseWithCompletion(
        activity: Activity,
        params: PaywallPurchaseLogicParams,
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
}

@Suppress("unused", "DEPRECATION")
private class DeprecatedPurchaseLogicSuspendAPI : PurchaseLogic {

    override suspend fun performPurchase(activity: Activity, rcPackage: Package): PurchaseLogicResult {
        return PurchaseLogicResult.Success
    }

    override suspend fun performRestore(customerInfo: CustomerInfo): PurchaseLogicResult {
        return PurchaseLogicResult.Success
    }
}

@Suppress("unused", "DEPRECATION")
private class DeprecatedPurchaseLogicCallbackAPI : PurchaseLogicWithCallback() {

    override fun performPurchaseWithCompletion(
        activity: Activity,
        rcPackage: Package,
        completion: (PurchaseLogicResult) -> Unit,
    ) {
        completion(PurchaseLogicResult.Success)
    }

    override fun performRestoreWithCompletion(customerInfo: CustomerInfo, completion: (PurchaseLogicResult) -> Unit) {
        completion(PurchaseLogicResult.Success)
    }
}
