package com.revenuecat.apitester.java;

import android.content.Context;

import com.revenuecat.purchases.Purchases;

import java.util.concurrent.ExecutorService;

@SuppressWarnings({"unused"})
final class PurchasesAPI {

    static void checkConfiguration(final Context context,
                                   final ExecutorService executorService) {
        Purchases.configureInCustomEntitlementsComputationMode(context, "", "");
    }

    static void check(final Purchases purchases) {
        purchases.switchUser("newUserID");
    }
}
