package com.revenuecat.apitester.java.revenuecatui;


import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultCaller;
import androidx.fragment.app.Fragment;

import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue;
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLaunchIfNeededOptions;
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLaunchOptions;
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher;
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallDisplayCallback;
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler;
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unused"})
final class PaywallActivityLauncherAPI {

    static void check(
            ComponentActivity activity,
            Fragment fragment,
            ActivityResultCaller resultCaller,
            PaywallResultHandler resultHandler,
            Offering offering,
            ParcelizableFontProvider fontProvider,
            PaywallDisplayCallback paywallDisplayCallback
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
        launcher.launch(null, null, true, true);
        launcher.launchIfNeeded("requiredEntitlementIdentifier");
        launcher.launchIfNeeded("requiredEntitlementIdentifier", offering);
        launcher.launchIfNeeded("requiredEntitlementIdentifier", null);
        launcher.launchIfNeeded("requiredEntitlementIdentifier", offering, fontProvider, true);
        launcher.launchIfNeeded("requiredEntitlementIdentifier", offering, null, true);
        launcher.launchIfNeeded("requiredEntitlementIdentifier", null, fontProvider, true);
        launcher.launchIfNeeded("requiredEntitlementIdentifier", null, null, true);
        launcher.launchIfNeeded("requiredEntitlementIdentifier", offering, fontProvider, true, true);
        launcher.launchIfNeeded("requiredEntitlementIdentifier", offering, fontProvider, true, true, paywallDisplayCallback);
        launcher.launchIfNeeded(offering, fontProvider, true, customerInfo -> null);
        launcher.launchIfNeeded(offering, null, true, customerInfo -> null);
        launcher.launchIfNeeded(null, fontProvider, true, customerInfo -> null);
        launcher.launchIfNeeded(null, null, true, customerInfo -> null);
        launcher.launchIfNeeded(null, null, true, true, customerInfo -> null);
    }

    static void checkBuilderPattern(
            PaywallActivityLauncher launcher,
            Offering offering,
            ParcelizableFontProvider fontProvider,
            PaywallDisplayCallback paywallDisplayCallback
    ) {
        Map<String, CustomVariableValue> customVariables = new HashMap<>();
        customVariables.put("key", new CustomVariableValue.String("value"));

        // Basic launch with builder
        PaywallActivityLaunchOptions options = new PaywallActivityLaunchOptions.Builder()
                .setOffering(offering)
                .setFontProvider(fontProvider)
                .setShouldDisplayDismissButton(true)
                .setEdgeToEdge(true)
                .setCustomVariables(customVariables)
                .build();
        launcher.launchWithOptions(options);

        // LaunchIfNeeded with requiredEntitlementIdentifier (all builder methods)
        PaywallActivityLaunchIfNeededOptions optionsWithEntitlement = new PaywallActivityLaunchIfNeededOptions.Builder()
                .setRequiredEntitlementIdentifier("premium")
                .setOffering(offering)
                .setFontProvider(fontProvider)
                .setShouldDisplayDismissButton(true)
                .setEdgeToEdge(true)
                .setCustomVariables(customVariables)
                .setPaywallDisplayCallback(paywallDisplayCallback)
                .build();
        launcher.launchIfNeededWithOptions(optionsWithEntitlement);

        // LaunchIfNeeded with shouldDisplayBlock (all builder methods)
        PaywallActivityLaunchIfNeededOptions optionsWithBlock = new PaywallActivityLaunchIfNeededOptions.Builder()
                .setShouldDisplayBlock(customerInfo -> customerInfo.getEntitlements().getActive().isEmpty())
                .setOffering(offering)
                .setFontProvider(fontProvider)
                .setShouldDisplayDismissButton(true)
                .setEdgeToEdge(true)
                .setCustomVariables(customVariables)
                .setPaywallDisplayCallback(paywallDisplayCallback)
                .build();
        launcher.launchIfNeededWithOptions(optionsWithBlock);
    }

    static void checkPaywallDisplayCallback() {
        PaywallDisplayCallback callback = new PaywallDisplayCallback() {
            @Override
            public void onPaywallDisplayResult(boolean wasDisplayed) {
            }
        };
    }
}
