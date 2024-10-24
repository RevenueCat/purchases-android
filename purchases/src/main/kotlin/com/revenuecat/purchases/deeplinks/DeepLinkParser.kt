package com.revenuecat.purchases.deeplinks

import android.net.Uri
import com.revenuecat.purchases.common.debugLog

internal class DeepLinkParser {
    companion object {
        private const val REDEEM_WEB_PURCHASE_HOST = "redeem_web_purchase"
    }

    sealed class DeepLink {
        data class RedeemWebPurchase(val redemptionToken: String) : DeepLink()
    }

    @Suppress("ReturnCount")
    fun parseDeepLink(data: Uri): DeepLink? {
        if (data.host == REDEEM_WEB_PURCHASE_HOST) {
            val redemptionToken = data.getQueryParameter("redemption_token")
            if (redemptionToken.isNullOrBlank()) {
                debugLog("Redemption token is missing web redemption deep link. Ignoring.")
                return null
            }
            return DeepLink.RedeemWebPurchase(redemptionToken)
        } else {
            debugLog("Unrecognized deep link host: ${data.host}. Ignoring")
            return null
        }
    }
}
