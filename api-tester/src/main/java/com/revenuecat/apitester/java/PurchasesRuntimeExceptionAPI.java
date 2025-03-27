package com.revenuecat.apitester.java;

import com.revenuecat.purchases.NoCoreLibraryDesugaringException;

@SuppressWarnings({"unused"})
final class PurchasesRuntimeExceptionAPI {
    static void check() {
        final String message = "message";
        final Throwable cause = new Throwable();
        throw new NoCoreLibraryDesugaringException(message, cause);
    }
}
