package com.revenuecat.apitester.kotlin

import android.content.Context
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfigurationForCustomEntitlementsComputationMode

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock")
private class PurchasesAPI {

    @Suppress("ForbiddenComment")
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun checkConfiguration(context: Context) {
        val configured: Boolean = Purchases.isConfigured

        Purchases.configureInCustomEntitlementsComputationMode(context, apiKey = "", appUserID = "")
        Purchases.configureInCustomEntitlementsComputationMode(
            PurchasesConfigurationForCustomEntitlementsComputationMode
                .Builder(
                    context = context,
                    apiKey = "",
                    appUserID = "",
                )
                .applyObfuscatedAccountIdToSubscriptionChanges(true)
                .showInAppMessagesAutomatically(false)
                .pendingTransactionsForPrepaidPlansEnabled(false)
                .build(),
        )
    }

    fun check(purchases: Purchases) {
        purchases.switchUser("newUserID")
    }
}
