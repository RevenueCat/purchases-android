package com.revenuecat.purchases.deeplinks

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.CustomerInfoUpdateHandler
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class WebPurchaseRedemptionHelper(
    private val backend: Backend,
    private val identityManager: IdentityManager,
    private val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val customerInfoUpdateHandler: CustomerInfoUpdateHandler,
    private val mainHandler: Handler? = Handler(Looper.getMainLooper()),
) {
    fun handleRedeemWebPurchase(
        deepLink: Purchases.DeepLink.WebPurchaseRedemption,
        listener: RedeemWebPurchaseListener,
    ) {
        debugLog("Starting web purchase redemption.")
        backend.postRedeemWebPurchase(
            identityManager.currentAppUserID,
            deepLink.redemptionToken,
            onErrorHandler = {
                errorLog("Error redeeming web purchase: $it")
                dispatchResult(listener, RedeemWebPurchaseListener.Result.Error(it))
            },
            onSuccessHandler = {
                debugLog("Successfully redeemed web purchase. Updating customer info.")
                offlineEntitlementsManager.resetOfflineCustomerInfoCache()
                customerInfoUpdateHandler.cacheAndNotifyListeners(it)
                dispatchResult(listener, RedeemWebPurchaseListener.Result.Success(it))
            },
        )
    }

    private fun dispatchResult(
        resultListener: RedeemWebPurchaseListener,
        result: RedeemWebPurchaseListener.Result,
    ) {
        dispatch { resultListener.handleResult(result) }
    }

    private fun dispatch(action: () -> Unit) {
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            val handler = mainHandler ?: Handler(Looper.getMainLooper())
            handler.post(action)
        } else {
            action()
        }
    }
}