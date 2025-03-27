package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.NoCoreLibraryDesugaringException

@Suppress("unused", "UNUSED_VARIABLE", "ThrowingExceptionsWithoutMessageOrCause")
private class NoCoreLibraryDesugaringExceptionAPI {
    fun check() {
        val cause = Throwable()
        val exception = NoCoreLibraryDesugaringException(cause)
        val runtimeException: RuntimeException = exception
        throw exception
    }
}
