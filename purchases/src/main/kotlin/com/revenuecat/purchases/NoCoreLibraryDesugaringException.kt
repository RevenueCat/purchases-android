package com.revenuecat.purchases

@JvmSynthetic
internal const val NO_CORE_LIBRARY_DESUGARING_ERROR_MESSAGE = "Error building BillingFlowParams which is required to " +
    "perform purchases in the Play store. This is due to an issue in Google's Billing Client library. " +
    "Please check https://errors.rev.cat/no-core-library-desugaring for more info and to fix this issue."

@Deprecated(
    "No longer thrown: the Billing Client 7 issue it reported does not affect Billing Client 8 and later.",
)
public class NoCoreLibraryDesugaringException(cause: Throwable) : RuntimeException(
    NO_CORE_LIBRARY_DESUGARING_ERROR_MESSAGE,
    cause,
)
