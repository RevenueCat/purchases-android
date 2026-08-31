package com.revenuecat.purchases

/**
 * Thrown when a reward verification status request fails.
 *
 * [isServerError] is true when the backend responded with an HTTP 5xx, which is transient and may
 * succeed on retry. Other failures (client errors, unrecognized backend codes) are deterministic.
 */
@InternalRevenueCatAPI
public class RewardVerificationException(
    error: PurchasesError,
    public val isServerError: Boolean,
) : PurchasesException(error)
