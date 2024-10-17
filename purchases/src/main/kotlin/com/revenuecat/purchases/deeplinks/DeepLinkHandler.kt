package com.revenuecat.purchases.deeplinks

import android.net.Uri
import com.revenuecat.purchases.common.debugLog

internal class DeepLinkHandler {
    companion object {
        private const val REDEEM_RCB_PURCHASE_HOST = "redeem_rcb_purchase"
    }

    sealed class DeepLink {
        data class RedeemRCBPurchase(val redemptionToken: String) : DeepLink()
    }

    @Suppress("ReturnCount")
    fun parseDeepLink(data: Uri): DeepLink? {
        if (data.host == REDEEM_RCB_PURCHASE_HOST) {
            val redemptionToken = data.getQueryParameter("redemption_token")
            if (redemptionToken.isNullOrBlank()) {
                debugLog("Redemption token is missing RCBilling redemption deep link. Ignoring.")
                return null
            }
            return DeepLink.RedeemRCBPurchase(redemptionToken)
        } else {
            debugLog("Unrecognized deep link host: ${data.host}. Ignoring")
            return null
        }
    }
}
