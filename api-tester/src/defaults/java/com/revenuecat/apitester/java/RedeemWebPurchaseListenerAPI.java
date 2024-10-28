package com.revenuecat.apitester.java;

import androidx.annotation.OptIn;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener;

@OptIn(markerClass = ExperimentalPreviewRevenueCatPurchasesAPI.class)
@SuppressWarnings({"unused"})
final class RedeemWebPurchaseListenerAPI {
    static void checkListener(RedeemWebPurchaseListener listener,
                              RedeemWebPurchaseListener.Result result) {
        listener.handleResult(result);
    }

    static void checkRedeemResult(RedeemWebPurchaseListener.Result result) {
        if (result instanceof RedeemWebPurchaseListener.Result.Success) {
            CustomerInfo customerInfo = ((RedeemWebPurchaseListener.Result.Success) result).getCustomerInfo();
        } else if (result instanceof RedeemWebPurchaseListener.Result.Error) {
            PurchasesError error = ((RedeemWebPurchaseListener.Result.Error) result).getError();
        }

        boolean isSuccess = result.isSuccess();
    }
}
