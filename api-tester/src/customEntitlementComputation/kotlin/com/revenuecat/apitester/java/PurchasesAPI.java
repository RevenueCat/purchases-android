package com.revenuecat.apitester.java;

import android.content.Context;

import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesConfigurationForCustomEntitlementsComputationMode;

import java.util.concurrent.ExecutorService;

@SuppressWarnings({"unused"})
final class PurchasesAPI {

    static void checkConfiguration(final Context context,
                                   final ExecutorService executorService) {
        Purchases.configureInCustomEntitlementsComputationMode(context, "", "");
        Purchases.configureInCustomEntitlementsComputationMode(
                new PurchasesConfigurationForCustomEntitlementsComputationMode.Builder(
                        context,
                        "",
                        ""
                ).showInAppMessagesAutomatically(false)
                        .pendingTransactionsForPrepaidPlansEnabled(false)
                        .build()
        );
    }

    static void check(final Purchases purchases) {
        purchases.switchUser("newUserID");
    }
}
