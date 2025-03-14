package com.revenuecat.apitester.kotlin

import android.content.Intent
import com.revenuecat.purchases.WebPurchaseRedemption
import com.revenuecat.purchases.asWebPurchaseRedemption

@Suppress("unused", "UNUSED_VARIABLE")
private class IntentExtensionsAPI {
    fun check(intent: Intent) {
        val webPurchaseRedemption: WebPurchaseRedemption? = intent.asWebPurchaseRedemption()
    }
}
