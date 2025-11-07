package com.revenuecat.apitester.java.revenuecatui;

import androidx.annotation.NonNull;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.models.StoreTransaction;
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener;
import com.revenuecat.purchases.ui.revenuecatui.utils.Resumable;

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
            public void onRestoreStarted() {}

            @Override
            public void onRestoreError(@NonNull PurchasesError error) {}

            @Override
            public void onRestoreCompleted(@NonNull CustomerInfo customerInfo) {}
        };
    }
}
