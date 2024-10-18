package com.revenuecat.apitester.java;

import com.revenuecat.purchases.interfaces.RedeemRCBillingPurchaseListener;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@SuppressWarnings({"unused"})
final class RedeemRCBillingPurchaseListenerAPI {
    static void checkListener(RedeemRCBillingPurchaseListener listener,
                              Function1<RedeemRCBillingPurchaseListener.ResultListener, Unit> startRedemption) {
        listener.handleRCBillingPurchaseRedemption(startRedemption);
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
