package com.revenuecat.purchases.deeplinks

import android.net.Uri
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.common.debugLog

internal class DeepLinkParser {
    companion object {
        private const val REDEEM_WEB_PURCHASE_HOST = "redeem_web_purchase"
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Suppress("ReturnCount")
    fun parseDeepLink(data: Uri): Purchases.DeepLink? {
        if (data.host == REDEEM_WEB_PURCHASE_HOST) {
            val redemptionToken = data.getQueryParameter("redemption_token")
            if (redemptionToken.isNullOrBlank()) {
                debugLog("Redemption token is missing web redemption deep link. Ignoring.")
                return null
            }
            return Purchases.DeepLink.WebRedemptionLink(redemptionToken)
        } else {
            debugLog("Unrecognized deep link host: ${data.host}. Ignoring")
            return null
        }
    }
}
