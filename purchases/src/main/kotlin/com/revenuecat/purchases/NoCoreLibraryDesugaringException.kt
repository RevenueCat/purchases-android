package com.revenuecat.purchases

class NoCoreLibraryDesugaringException(cause: Throwable) : RuntimeException(
    "Error building BillingFlowParams which is required to " +
        "perform purchases in the Play store. This is due to an issue in Google's Billing Client library. " +
        "Please check https://errors.rev.cat/no-core-library-desugaring for more info and to fix this issue.",
    cause,
)
