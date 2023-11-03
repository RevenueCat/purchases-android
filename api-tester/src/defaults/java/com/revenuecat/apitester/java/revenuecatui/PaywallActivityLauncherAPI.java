package com.revenuecat.apitester.java.revenuecatui;


import androidx.activity.ComponentActivity;
import androidx.fragment.app.Fragment;

import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI;
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher;
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler;
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider;

@SuppressWarnings({"unused"})
@ExperimentalPreviewRevenueCatUIPurchasesAPI
final class PaywallActivityLauncherAPI {

    static void check(
            ComponentActivity activity,
            Fragment fragment,
            PaywallResultHandler resultHandler,
            Offering offering,
            ParcelizableFontProvider fontProvider
    ) {
        PaywallActivityLauncher launcher = new PaywallActivityLauncher(activity, resultHandler);
        PaywallActivityLauncher launcher2 = new PaywallActivityLauncher(fragment, resultHandler);
        launcher.launch();
        launcher.launch(offering);
        launcher.launch(null, fontProvider);
        launcher.launch(null, null, true);
        launcher.launchIfNeeded("requiredEntitlementIdentifier");
        launcher.launchIfNeeded(null, null, true, customerInfo -> null);
    }
}
