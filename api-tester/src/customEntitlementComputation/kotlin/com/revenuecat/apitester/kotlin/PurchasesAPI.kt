package com.revenuecat.apitester.kotlin

import android.content.Context
import com.revenuecat.purchases.Purchases

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock")
private class PurchasesAPI {

    @Suppress("ForbiddenComment")
    fun checkConfiguration(context: Context) {
        val configured: Boolean = Purchases.isConfigured

        Purchases.configureInCustomEntitlementsMode(context, apiKey = "", appUserID = "")
    }

    fun check(purchases: Purchases) {
        purchases.switchUser("newUserID")
    }
}
