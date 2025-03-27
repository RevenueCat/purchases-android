package com.revenuecat.purchases

// TODO: Add link
class NoCoreLibraryDesugaringException(cause: Throwable) : RuntimeException(
    "Error building BillingFlowParams for subscriptions " +
        "due to an issue in Google's Billing Client library. Please check TODO-LINK to fix this issue.",
    cause,
)
