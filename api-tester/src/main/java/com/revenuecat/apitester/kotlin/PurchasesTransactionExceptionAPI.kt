package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesTransactionException

class PurchasesTransactionExceptionAPI {
    fun check(exception: PurchasesTransactionException) {
        val underlyingErrorMessage: String? = exception.underlyingErrorMessage
        val message: String = exception.message
        val code: PurchasesErrorCode = exception.code
        val error: PurchasesError = exception.error
        val userCancelled: Boolean = exception.userCancelled
    }
}
