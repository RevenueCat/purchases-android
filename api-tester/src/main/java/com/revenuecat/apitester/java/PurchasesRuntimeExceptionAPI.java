package com.revenuecat.apitester.java;

import com.revenuecat.purchases.PurchasesRuntimeException;

@SuppressWarnings({"unused"})
final class PurchasesRuntimeExceptionAPI {
    static void check() {
        final String message = "message";
        final Throwable cause = new Throwable();
        throw new PurchasesRuntimeException(message, cause);
    }
}
