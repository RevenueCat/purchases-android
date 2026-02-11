package com.revenuecat.purchases

open class PurchasesException internal constructor(
    public val error: PurchasesError,
    internal val overridenMessage: String? = null,
) : Exception() {

    constructor(error: PurchasesError) : this(error, null)

    public val code: PurchasesErrorCode
        get() = error.code

    public val underlyingErrorMessage: String?
        get() = error.underlyingErrorMessage

    override val message: String
        get() = overridenMessage ?: error.message
}
