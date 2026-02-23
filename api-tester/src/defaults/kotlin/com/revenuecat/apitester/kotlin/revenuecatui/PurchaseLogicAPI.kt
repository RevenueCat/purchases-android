package com.revenuecat.apitester.kotlin.revenuecatui

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ReplacementMode
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseContext
import com.revenuecat.purchases.ui.revenuecatui.ProductChange
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicWithCallback

@Suppress("unused", "UNUSED_VARIABLE")
private class PurchaseLogicAPI {

    suspend fun checkPurchase(
        mySuspendLogic: PurchaseLogic,
        activity: Activity,
        rcPackage: Package,
    ) {
        val suspendLogicPurchase: PurchaseLogicResult = mySuspendLogic.performPurchase(activity, rcPackage)
    }

    suspend fun checkPurchaseWithContext(
        mySuspendLogic: PurchaseLogic,
        activity: Activity,
        rcPackage: Package,
    ) {
        val context = PaywallPurchaseContext(rcPackage = rcPackage, productChange = null, subscriptionOption = null)
        val result: PurchaseLogicResult = mySuspendLogic.performPurchase(activity, context)
    }

    suspend fun checkRestore(mySuspendLogic: PurchaseLogic, customerInfo: CustomerInfo) {
        val suspendLogicRestore: PurchaseLogicResult = mySuspendLogic.performRestore(customerInfo)
    }

    fun checkProductChange() {
        val productChange = ProductChange(
            oldProductId = "old_product_id",
            replacementMode = GoogleReplacementMode.CHARGE_PRORATED_PRICE,
        )
        val productChangeNoReplacementMode = ProductChange(
            oldProductId = "old_product_id",
            replacementMode = null,
        )
        val oldProductId: String = productChange.oldProductId
        val replacementMode: ReplacementMode? = productChange.replacementMode
    }

    fun checkPaywallPurchaseContext(rcPackage: Package, subscriptionOption: SubscriptionOption) {
        val context = PaywallPurchaseContext(
            rcPackage = rcPackage,
            productChange = ProductChange(
                oldProductId = "old_product_id",
                replacementMode = GoogleReplacementMode.DEFERRED,
            ),
            subscriptionOption = subscriptionOption,
        )
        val pkg: Package = context.rcPackage
        val productChange: ProductChange? = context.productChange
        val option: SubscriptionOption? = context.subscriptionOption
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

@Suppress("unused")
private class PurchaseLogicWithCallbackAndContextAPI : PurchaseLogicWithCallback() {

    override fun performPurchaseWithCompletion(
        activity: Activity,
        rcPackage: Package,
        completion: (PurchaseLogicResult) -> Unit,
    ) {
        completion(PurchaseLogicResult.Success)
    }

    override fun performPurchaseWithCompletion(
        activity: Activity,
        context: PaywallPurchaseContext,
        completion: (PurchaseLogicResult) -> Unit,
    ) {
        completion(PurchaseLogicResult.Success)
    }

    override fun performRestoreWithCompletion(customerInfo: CustomerInfo, completion: (PurchaseLogicResult) -> Unit) {
        completion(PurchaseLogicResult.Success)
    }
}
