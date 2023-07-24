package com.revenuecat.apitester.java;

import android.content.Context;

import com.revenuecat.purchases.EntitlementVerificationMode;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesConfiguration;

import java.util.concurrent.ExecutorService;

@SuppressWarnings({"unused"})
final class PurchasesAPI {

    static void checkConfiguration(final Context context,
                                   final ExecutorService executorService) {
        Purchases.configureInCustomEntitlementsMode(context, "", "");
    }

    static void check(final Purchases purchases) {
        purchases.switchUser("newUserID");
    }
}
