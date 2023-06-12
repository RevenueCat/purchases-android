package com.revenuecat.apitester.java;

import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.PurchasesErrorCode;
import com.revenuecat.purchases.PurchasesException;

final class PurchasesExceptionAPI {
    static void check(final PurchasesException exception) {
        final String underlyingErrorMessage = exception.getUnderlyingErrorMessage();
        final String message = exception.getMessage();
        final PurchasesErrorCode code = exception.getCode();
        final PurchasesError error = exception.getError();
    }
}