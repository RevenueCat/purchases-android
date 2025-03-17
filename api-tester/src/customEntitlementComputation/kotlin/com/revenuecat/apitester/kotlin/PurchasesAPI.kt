package com.revenuecat.apitester.kotlin

import android.content.Context
import com.revenuecat.purchases.Purchases

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock")
private class PurchasesAPI {

    @Suppress("ForbiddenComment")
    fun checkConfiguration(context: Context) {
        val configured: Boolean = Purchases.isConfigured

        Purchases.configureInCustomEntitlementsComputationMode(context, apiKey = "", appUserID = "")
        Purchases.configureInCustomEntitlementsComputationMode(
            context,
            apiKey = "",
            appUserID = "",
            showInAppMessagesAutomatically = false,
        )
        Purchases.configureInCustomEntitlementsComputationMode(
            context,
            apiKey = "",
            appUserID = "",
            pendingTransactionsForPrepaidPlansEnabled = false,
        )
        Purchases.configureInCustomEntitlementsComputationMode(
            context,
            apiKey = "",
            appUserID = "",
            showInAppMessagesAutomatically = false,
            pendingTransactionsForPrepaidPlansEnabled = false,
        )
    }

    fun check(purchases: Purchases) {
        purchases.switchUser("newUserID")
    }
}
