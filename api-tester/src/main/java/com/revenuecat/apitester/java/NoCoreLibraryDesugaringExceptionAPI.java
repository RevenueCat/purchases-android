package com.revenuecat.apitester.java;

import com.revenuecat.purchases.NoCoreLibraryDesugaringException;

@SuppressWarnings({"unused"})
final class NoCoreLibraryDesugaringExceptionAPI {
    static void check() {
        final Throwable cause = new Throwable();
        throw new NoCoreLibraryDesugaringException(cause);
    }
}
