package com.revenuecat.purchases.deeplinks

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.CustomerInfoUpdateHandler
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.deeplinks.DeepLinkParser.DeepLink
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
    var redeemWebPurchaseListener: RedeemWebPurchaseListener? = null

    fun handleRedeemWebPurchase(deepLink: DeepLink.RedeemWebPurchase): Boolean {
        val listener = redeemWebPurchaseListener ?: run {
            errorLog("No RedeemWebPurchaseListener set. Ignoring Web purchase redemption.")
            return false
        }
        debugLog("Detected web purchase redemption. Asking callback to initiate redemption.")
        listener.handleWebPurchaseRedemption(object : RedeemWebPurchaseListener.WebPurchaseRedeemer {
            override fun redeemWebPurchase(resultListener: RedeemWebPurchaseListener.ResultListener) {
                debugLog("Starting redeeming web purchase.")
                backend.postRedeemWebPurchase(
                    identityManager.currentAppUserID,
                    deepLink.redemptionToken,
                    onErrorHandler = {
                        errorLog("Error redeeming web purchase: $it")
                        handleResult(resultListener, RedeemWebPurchaseListener.Result.Error(it))
                    },
                    onSuccessHandler = {
                        debugLog("Successfully redeemed web purchase. Updating customer info.")
                        offlineEntitlementsManager.resetOfflineCustomerInfoCache()
                        customerInfoUpdateHandler.cacheAndNotifyListeners(it)
                        handleResult(resultListener, RedeemWebPurchaseListener.Result.Success(it))
                    },
                )
            }
        })
        return true
    }

    private fun handleResult(
        resultListener: RedeemWebPurchaseListener.ResultListener,
        result: RedeemWebPurchaseListener.Result,
    ) {
        dispatchResult { resultListener.handleResult(result) }
    }

    private fun dispatchResult(action: () -> Unit) {
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            val handler = mainHandler ?: Handler(Looper.getMainLooper())
            handler.post(action)
        } else {
            action()
        }
    }
}
