package com.revenuecat.apitester.java;

import com.revenuecat.purchases.DangerousSettings;

@SuppressWarnings({"unused"})
final class DangerousSettingsAPI {
    static void check(final DangerousSettings dangerousSettings) {
        final boolean autoSync = dangerousSettings.getAutoSyncPurchases();
        final boolean disableRequiredSignatureVerifications =
                dangerousSettings.getDisableRequiredSignatureVerifications();
        dangerousSettings.forceAllowTestStoreInReleaseBuilds();
    }

    static void checkConstructors() {
        final DangerousSettings defaults = new DangerousSettings();
        final DangerousSettings autoSync = new DangerousSettings(false);
        final DangerousSettings skipping = new DangerousSettings(true, true);
    }
}
