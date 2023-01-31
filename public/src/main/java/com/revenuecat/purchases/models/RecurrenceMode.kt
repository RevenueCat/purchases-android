package com.revenuecat.purchases.models

/**
 * Recurrence mode for a pricing phase
 */
// TODO if google-only, rename and annotate identifier with @ProductDetails.RecurrenceMode
// TODO add api testers
enum class RecurrenceMode(val identifier: Int?) {

    // Pricing phase does not repeat
    NON_RECURRING(0),

    // Pricing phase repeats infinitely until cancellation
    INFINITE_RECURRING(1),

    // Pricing phase repeats for a fixed number of billing periods
    FINITE_RECURRING(2),
    UNKNOWN(null)
}

fun Int?.toRecurrenceMode(): RecurrenceMode =
    RecurrenceMode.values().firstOrNull { it.identifier == this } ?: RecurrenceMode.UNKNOWN
