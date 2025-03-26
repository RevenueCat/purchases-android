package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.PurchasesRuntimeException

@Suppress("unused", "UNUSED_VARIABLE", "ThrowingExceptionsWithoutMessageOrCause")
private class PurchasesRuntimeExceptionAPI {
    fun check() {
        val message = ""
        val cause = Throwable()
        val exception = PurchasesRuntimeException(message, cause)
        val runtimeException: RuntimeException = exception
        throw exception
    }
}
