package com.revenuecat.apitester.java;

import androidx.annotation.OptIn;

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI;
import com.revenuecat.purchases.interfaces.RedeemRCBillingPurchaseListener;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@OptIn(markerClass = ExperimentalPreviewRevenueCatPurchasesAPI.class)
@SuppressWarnings({"unused"})
final class RedeemRCBillingPurchaseListenerAPI {
    static void checkListener(RedeemRCBillingPurchaseListener listener,
                              RedeemRCBillingPurchaseListener.RedemptionStarter redemptionStarter) {
        listener.handleRCBillingPurchaseRedemption(redemptionStarter);
    }

    static void checkRedemptionStarter(RedeemRCBillingPurchaseListener.RedemptionStarter redemptionStarter,
                                       RedeemRCBillingPurchaseListener.ResultListener resultListener) {
        redemptionStarter.startRedemption(resultListener);
    }

    static void checkResultListener(RedeemRCBillingPurchaseListener.ResultListener resultListener,
                                    RedeemRCBillingPurchaseListener.RedeemResult result) {
        resultListener.handleResult(result);
    }

    static void checkRedeemResult(RedeemRCBillingPurchaseListener.RedeemResult result) {
        switch (result) {
            case SUCCESS:
            case ERROR:
                break;
        }
    }
}
