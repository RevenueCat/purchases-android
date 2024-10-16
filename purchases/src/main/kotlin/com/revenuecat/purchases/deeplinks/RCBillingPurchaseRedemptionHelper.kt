package com.revenuecat.purchases.deeplinks

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfoHelper
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.deeplinks.DeepLinkHandler.DeepLink
import com.revenuecat.purchases.identity.IdentityManager

internal class RCBillingPurchaseRedemptionHelper(
    private val backend: Backend,
    private val identityManager: IdentityManager,
    private val customerInfoHelper: CustomerInfoHelper,
) {
    fun handleRedeemRCBPurchase(deepLink: DeepLink.RedeemRCBPurchase) {
        debugLog("Redeeming RCBilling purchase with redemption token: ${deepLink.redemptionToken}")
        backend.postRedeemRCBillingPurchase(
            // WIP: These parameters are the other way around because the aliasing endpoint doesn't seem to work if
            // IDs are passed the other way around.
            deepLink.redemptionToken,
            identityManager.currentAppUserID,
            onErrorHandler = {
                errorLog("Error redeeming RCBilling purchase: $it")
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
                )
            },
        )
    }
}
