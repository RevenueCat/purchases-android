package com.revenuecat.purchases

// TODO: Add link
class NoCoreLibraryDesugaringException(cause: Throwable) : RuntimeException(
    "Error building BillingFlowParams which is required to " +
        "perform purchases in the Play store. This is due to an issue in Google's Billing Client library. " +
        "Please check TODO-LINK for more info and to fix this issue.",
    cause,
)
