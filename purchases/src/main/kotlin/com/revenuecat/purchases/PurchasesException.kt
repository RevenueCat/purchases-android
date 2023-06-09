package com.revenuecat.purchases

class PurchasesException(val error: PurchasesError) : Exception() {

    val code: PurchasesErrorCode
        get() = error.code

    val underlyingErrorMessage: String?
        get() = error.underlyingErrorMessage

    override val message: String
        get() = error.message
}
