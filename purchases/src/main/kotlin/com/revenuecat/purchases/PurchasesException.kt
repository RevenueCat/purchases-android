package com.revenuecat.purchases

public open class PurchasesException(public val error: PurchasesError) : Exception() {

    public val code: PurchasesErrorCode
        get() = error.code

    public val underlyingErrorMessage: String?
        get() = error.underlyingErrorMessage

    override val message: String
        get() = error.message
}
