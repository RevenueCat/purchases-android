package com.revenuecat.purchases.deeplinks

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.CustomerInfoHelper
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.deeplinks.DeepLinkParser.DeepLink
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.RedeemRCBillingPurchaseListener

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class RCBillingPurchaseRedemptionHelper(
    private val backend: Backend,
    private val identityManager: IdentityManager,
    private val customerInfoHelper: CustomerInfoHelper,
    private val mainHandler: Handler? = Handler(Looper.getMainLooper()),
) {
    var redeemRCBillingPurchaseListener: RedeemRCBillingPurchaseListener? = null

    fun handleRedeemRCBPurchase(deepLink: DeepLink.RedeemRCBPurchase): Boolean {
        val listener = redeemRCBillingPurchaseListener ?: run {
            errorLog("No RedeemRCBillingPurchaseListener set. Ignoring RCBilling purchase redemption.")
            return false
        }
        debugLog("Detected RCB purchase redemption. Asking callback to initiate redemption.")
        listener.handleRCBillingPurchaseRedemption(object : RedeemRCBillingPurchaseListener.RedemptionStarter {
            override fun startRedemption(resultListener: RedeemRCBillingPurchaseListener.ResultListener) {
                debugLog("Redeeming RCBilling purchase with redemption token: ${deepLink.redemptionToken}")
                backend.postRedeemRCBillingPurchase(
                    // WIP: These parameters are the other way around because the aliasing endpoint doesn't seem to
                    // work if IDs are passed the other way around.
                    deepLink.redemptionToken,
                    identityManager.currentAppUserID,
                    onErrorHandler = {
                        errorLog("Error redeeming RCBilling purchase: $it")
                        handleResult(resultListener, RedeemRCBillingPurchaseListener.RedeemResult.ERROR)
                    },
                    onSuccessHandler = {
                        // WIP: This ideally shouldn't be needed and the redemption endpoint should give us the customer
                        // info directly
                        debugLog("Successfully redeemed RCBilling purchase. Updating customer info.")
                        customerInfoHelper.retrieveCustomerInfo(
                            identityManager.currentAppUserID,
                            fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
                            appInBackground = false,
                            // This is wrong but we should be removing this call soon.
                            allowSharingPlayStoreAccount = identityManager.currentUserIsAnonymous(),
                            callback = object : ReceiveCustomerInfoCallback {
                                override fun onReceived(customerInfo: CustomerInfo) {
                                    handleResult(resultListener, RedeemRCBillingPurchaseListener.RedeemResult.SUCCESS)
                                }

                                override fun onError(error: PurchasesError) {
                                    handleResult(resultListener, RedeemRCBillingPurchaseListener.RedeemResult.ERROR)
                                }
                            },
                        )
                    },
                )
            }
        })
        return true
    }

    private fun handleResult(
        resultListener: RedeemRCBillingPurchaseListener.ResultListener,
        result: RedeemRCBillingPurchaseListener.RedeemResult,
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
