package com.revenuecat.apitester.java;

import androidx.annotation.OptIn;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.RedeemWebResultListener;

@OptIn(markerClass = ExperimentalPreviewRevenueCatPurchasesAPI.class)
@SuppressWarnings({"unused"})
final class RedeemWebResultListenerAPI {
    static void checkListener(RedeemWebResultListener listener,
                              RedeemWebResultListener.Result result) {
        listener.handleResult(result);
    }

    static void checkRedeemResult(RedeemWebResultListener.Result result) {
        if (result instanceof RedeemWebResultListener.Result.Success) {
            CustomerInfo customerInfo = ((RedeemWebResultListener.Result.Success) result).getCustomerInfo();
        } else if (result instanceof RedeemWebResultListener.Result.Error) {
            PurchasesError error = ((RedeemWebResultListener.Result.Error) result).getError();
        }

        boolean isSuccess = result.isSuccess();
    }
}
