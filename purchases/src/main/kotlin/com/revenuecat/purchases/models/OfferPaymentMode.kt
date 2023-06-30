package com.revenuecat.purchases.models

/**
 * Payment mode for offer pricing phases
 */
enum class OfferPaymentMode {
    // Subscribers don't pay until the specified period ends
    FREE_TRIAL,

    // Subscribers pay up front for a specified period
    SINGLE_PAYMENT,

    // Subscribers pay a discounted amount for a specified number of periods
    DISCOUNTED_RECURRING_PAYMENT,
}
