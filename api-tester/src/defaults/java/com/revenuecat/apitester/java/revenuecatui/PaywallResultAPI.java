package com.revenuecat.apitester.java.revenuecatui;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.activity.result.ActivityResultCallback;
import androidx.annotation.NonNull;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI;
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult;
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler;

@SuppressWarnings({"unused"})
@ExperimentalPreviewRevenueCatUIPurchasesAPI
final class PaywallResultAPI {

    static void checkResultHandler(
            final PaywallResultHandler resultHandler
    ) {
        final ActivityResultCallback<PaywallResult> callback = resultHandler;
    }

    static void checkResult(PurchasesError error, CustomerInfo customerInfo) {
        final PaywallResult result = PaywallResult.Cancelled.INSTANCE;
        final Parcelable parcelable = result;
        final PaywallResult.Error result2 = new PaywallResult.Error(error);
        PurchasesError error2 = result2.getError();
        final PaywallResult.Purchased result3 = new PaywallResult.Purchased(customerInfo);
        CustomerInfo customerInfo2 = result3.getCustomerInfo();
        final PaywallResult result2AsSealedClass = result2;
        final PaywallResult result3AsSealedClass = result3;
    }
}
