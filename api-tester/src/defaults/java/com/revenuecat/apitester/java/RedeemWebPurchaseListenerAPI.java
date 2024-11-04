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
        } else if (result instanceof RedeemWebPurchaseListener.Result.InvalidToken) {
            return;
        } else if (result instanceof RedeemWebPurchaseListener.Result.AlreadyRedeemed) {
            return;
        } else if (result instanceof RedeemWebPurchaseListener.Result.Expired) {
            String obfuscatedEmail = ((RedeemWebPurchaseListener.Result.Expired) result).getObfuscatedEmail();
            Boolean wasEmailSent = ((RedeemWebPurchaseListener.Result.Expired) result).getWasEmailSent();
        }

        boolean isSuccess = result.isSuccess();
    }
}
