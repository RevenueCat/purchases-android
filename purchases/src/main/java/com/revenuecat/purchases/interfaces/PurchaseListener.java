package com.revenuecat.purchases.interfaces;

import androidx.annotation.NonNull;

import com.revenuecat.purchases.PurchasesError;

public interface PurchaseListener {
    /**
     * Will be called after the product change has completed with error
     * @param error A [PurchasesError] containing the reason for the failure when making the product change
     * @param userCancelled A boolean indicating if the user cancelled the purchase. In that case the error will also be
     *                     [PurchasesErrorCode.PurchaseCancelledError]
     */
    void onError(@NonNull PurchasesError error, boolean userCancelled);
}
