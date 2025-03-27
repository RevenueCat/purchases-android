package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.NoCoreLibraryDesugaringException

@Suppress("unused", "UNUSED_VARIABLE", "ThrowingExceptionsWithoutMessageOrCause")
private class PurchasesRuntimeExceptionAPI {
    fun check() {
        val message = ""
        val cause = Throwable()
        val exception = NoCoreLibraryDesugaringException(message, cause)
        val runtimeException: RuntimeException = exception
        throw exception
    }
}
