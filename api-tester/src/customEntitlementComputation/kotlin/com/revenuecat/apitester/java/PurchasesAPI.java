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
        PurchasesConfiguration build = new PurchasesConfiguration.Builder(context, "", "")
                .observerMode(true)
                .observerMode(false)
                .service(executorService)
                .diagnosticsEnabled(true)
                .entitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)
                .build();

        Purchases.configure(build);
    }
}
