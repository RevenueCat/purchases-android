package com.revenuecat.apitester.java;

import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.PurchasesErrorCode;
import com.revenuecat.purchases.PurchasesTransactionException;

final class PurchasesTransactionExceptionAPI {
    static void check(final PurchasesTransactionException exception) {
        final String underlyingErrorMessage = exception.getUnderlyingErrorMessage();
        final String message = exception.getMessage();
        final PurchasesErrorCode code = exception.getCode();
        final PurchasesError error = exception.getError();
        final boolean userCancelled = exception.getUserCancelled();
    }
}