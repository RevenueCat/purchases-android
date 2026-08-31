package com.revenuecat.purchases

/**
 * Internal data describing a failed reward verification status request.
 *
 * Mirrors the [PurchasesError] / [PurchasesException] split: the backend and callbacks pass this data
 * type, and the suspend API converts it into a thrown [RewardVerificationException] at the boundary.
 *
 * [isServerError] is true when the backend responded with an HTTP 5xx (transient).
 */
internal data class RewardVerificationError(
    val error: PurchasesError,
    val isServerError: Boolean,
)
