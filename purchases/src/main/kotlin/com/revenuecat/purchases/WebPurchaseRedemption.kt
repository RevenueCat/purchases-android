package com.revenuecat.purchases

/**
 * Represents a web redemption link, that can be redeemed using [Purchases.redeemWebPurchase]
 */
class WebPurchaseRedemption internal constructor(
    internal val redemptionToken: String,
)
