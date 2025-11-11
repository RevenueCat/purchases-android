package com.revenuecat.purchases

open class PurchasesException internal constructor(
    val error: PurchasesError,
    internal val overridenMessage: String? = null,
) : Exception() {

    constructor(error: PurchasesError) : this(error, null)

    val code: PurchasesErrorCode
        get() = error.code

    val underlyingErrorMessage: String?
        get() = error.underlyingErrorMessage

    override val message: String
        get() = overridenMessage ?: error.message
}
