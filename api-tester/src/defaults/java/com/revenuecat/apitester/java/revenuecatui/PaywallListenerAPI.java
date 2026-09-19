package com.revenuecat.apitester.java.revenuecatui;

import androidx.annotation.NonNull;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.models.StoreTransaction;
import com.revenuecat.purchases.ui.revenuecatui.PaywallInteractionEvent;
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener;
import com.revenuecat.purchases.ui.revenuecatui.utils.Resumable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
final class PaywallListenerAPI {
    void check() {
        PaywallListener listener = new PaywallListener() {
            @Override
            public void onPurchasePackageInitiated(@NonNull Package rcPackage, @NonNull Resumable resume) {}

            @Override
            public void onPurchaseStarted(@NonNull Package rcPackage) {}

            @Override
            public void onPurchaseError(@NonNull PurchasesError error) {}

            @Override
            public void onPurchaseCompleted(@NonNull CustomerInfo customerInfo, @NonNull StoreTransaction storeTransaction) {}

            @Override
            public void onPurchaseCancelled() {}

            @Override
            public void onRestoreInitiated(@NonNull Resumable resume) {}

            @Override
            public void onRestoreStarted() {}

            @Override
            public void onRestoreError(@NonNull PurchasesError error) {}

            @Override
            public void onRestoreCompleted(@NonNull CustomerInfo customerInfo) {}

            @Override
            public void onWebCheckoutOpened() {}

            @Override
            public void onUrlOpened(@NonNull String url) {}

            @Override
            public void onInteraction(@NonNull PaywallInteractionEvent event) {
                Map<String, Object> rawProperties = event.getRawProperties();
                String string = event.getProperty(PaywallInteractionEvent.Keys.COMPONENT_TYPE);
                Integer integer = event.getProperty(PaywallInteractionEvent.Keys.ORIGIN_INDEX);
                Long longValue = event.getProperty(PaywallInteractionEvent.Keys.TIMESTAMP);
                Boolean bool = event.getProperty(PaywallInteractionEvent.Keys.DARK_MODE);
                String name = PaywallInteractionEvent.Keys.COMPONENT_TYPE.getName();
            }
        };

        List<PaywallInteractionEvent.Key<?>> keys = Arrays.asList(
                PaywallInteractionEvent.Keys.TIMESTAMP,
                PaywallInteractionEvent.Keys.SESSION_ID,
                PaywallInteractionEvent.Keys.OFFERING_ID,
                PaywallInteractionEvent.Keys.PAYWALL_ID,
                PaywallInteractionEvent.Keys.PAYWALL_REVISION,
                PaywallInteractionEvent.Keys.DISPLAY_MODE,
                PaywallInteractionEvent.Keys.DARK_MODE,
                PaywallInteractionEvent.Keys.LOCALE,
                PaywallInteractionEvent.Keys.COMPONENT_TYPE,
                PaywallInteractionEvent.Keys.COMPONENT_VALUE,
                PaywallInteractionEvent.Keys.COMPONENT_NAME,
                PaywallInteractionEvent.Keys.COMPONENT_URL,
                PaywallInteractionEvent.Keys.ORIGIN_INDEX,
                PaywallInteractionEvent.Keys.DESTINATION_INDEX,
                PaywallInteractionEvent.Keys.ORIGIN_CONTEXT_NAME,
                PaywallInteractionEvent.Keys.DESTINATION_CONTEXT_NAME,
                PaywallInteractionEvent.Keys.DEFAULT_INDEX,
                PaywallInteractionEvent.Keys.ORIGIN_PACKAGE_ID,
                PaywallInteractionEvent.Keys.DESTINATION_PACKAGE_ID,
                PaywallInteractionEvent.Keys.DEFAULT_PACKAGE_ID,
                PaywallInteractionEvent.Keys.CURRENT_PACKAGE_ID,
                PaywallInteractionEvent.Keys.RESULTING_PACKAGE_ID,
                PaywallInteractionEvent.Keys.ORIGIN_PRODUCT_ID,
                PaywallInteractionEvent.Keys.DESTINATION_PRODUCT_ID,
                PaywallInteractionEvent.Keys.DEFAULT_PRODUCT_ID,
                PaywallInteractionEvent.Keys.CURRENT_PRODUCT_ID,
                PaywallInteractionEvent.Keys.RESULTING_PRODUCT_ID
        );
        List<String> componentTypes = Arrays.asList(
                PaywallInteractionEvent.ComponentTypes.TAB,
                PaywallInteractionEvent.ComponentTypes.SWITCH,
                PaywallInteractionEvent.ComponentTypes.CAROUSEL,
                PaywallInteractionEvent.ComponentTypes.BUTTON,
                PaywallInteractionEvent.ComponentTypes.TEXT,
                PaywallInteractionEvent.ComponentTypes.PACKAGE,
                PaywallInteractionEvent.ComponentTypes.PACKAGE_SELECTION_SHEET,
                PaywallInteractionEvent.ComponentTypes.PURCHASE_BUTTON
        );

        // Only compiles with -Xjvm-default=all-compatibility; guards against method additions
        // becoming source-breaking for Java implementors.
        PaywallListener listenerWithDefaults = new PaywallListener() {};
    }
}
