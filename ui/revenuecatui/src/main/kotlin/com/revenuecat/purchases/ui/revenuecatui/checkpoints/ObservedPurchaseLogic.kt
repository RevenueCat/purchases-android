package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogicParams
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult

/**
 * The app's [PaywallPurchaseLogic] as a checkpoint paywall sees it: every call goes to [delegate], and a purchase
 * or restore the app reports as successful is also reported to [onCompleted]. A paywall whose purchases the app
 * completes tells its listener nothing about them, so this is where a checkpoint learns that the user went
 * through. Called on the paywall's thread, the main one, before the paywall syncs the purchase.
 */
internal class ObservedPurchaseLogic(
    val delegate: PaywallPurchaseLogic,
    private val onCompleted: () -> Unit,
) : PaywallPurchaseLogic {

    override suspend fun performPurchase(
        activity: Activity,
        params: PaywallPurchaseLogicParams,
    ): PurchaseLogicResult = delegate.performPurchase(activity, params).also(::observe)

    override suspend fun performRestore(customerInfo: CustomerInfo): PurchaseLogicResult =
        delegate.performRestore(customerInfo).also(::observe)

    private fun observe(result: PurchaseLogicResult) {
        if (result is PurchaseLogicResult.Success) onCompleted()
    }
}
