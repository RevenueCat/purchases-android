package com.revenuecat.apitester.java.revenuecatui;


import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultCaller;
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
            ActivityResultCaller resultCaller,
            PaywallResultHandler resultHandler,
            Offering offering,
            ParcelizableFontProvider fontProvider
    ) {
        PaywallActivityLauncher launcher = new PaywallActivityLauncher(activity, resultHandler);
        PaywallActivityLauncher launcher2 = new PaywallActivityLauncher(fragment, resultHandler);
        PaywallActivityLauncher launcher3 = new PaywallActivityLauncher(resultCaller, resultHandler);
        launcher.launch();
        launcher.launch(offering);
        launcher.launch(null);
        launcher.launch(offering, fontProvider);
        launcher.launch(offering, null);
        launcher.launch(null, fontProvider);
        launcher.launch(null, null);
        launcher.launch(offering, fontProvider, true);
        launcher.launch(offering, null, true);
        launcher.launch(null, fontProvider, true);
        launcher.launch(null, null, true);
        launcher.launchIfNeeded("requiredEntitlementIdentifier");
        launcher.launchIfNeeded("requiredEntitlementIdentifier", offering);
        launcher.launchIfNeeded("requiredEntitlementIdentifier", null);
        launcher.launchIfNeeded("requiredEntitlementIdentifier", offering, fontProvider, true);
        launcher.launchIfNeeded("requiredEntitlementIdentifier", offering, null, true);
        launcher.launchIfNeeded("requiredEntitlementIdentifier", null, fontProvider, true);
        launcher.launchIfNeeded("requiredEntitlementIdentifier", null, null, true);
        launcher.launchIfNeeded(offering, fontProvider, true, customerInfo -> null);
        launcher.launchIfNeeded(offering, null, true, customerInfo -> null);
        launcher.launchIfNeeded(null, fontProvider, true, customerInfo -> null);
        launcher.launchIfNeeded(null, null, true, customerInfo -> null);
    }
}
