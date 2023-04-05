package com.revenuecat.purchases.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Encapsulates how a user pays for a subscription at a given point in time.
 */
@Parcelize
data class PricingPhase(
    /**
     * Billing period for which the [PricingPhase] applies.
     */
    val billingPeriod: Period,

    /**
     * [RecurrenceMode] of the [PricingPhase]
     */
    val recurrenceMode: RecurrenceMode,

    /**
     * Number of cycles for which the pricing phase applies.
     * Null for INFINITE_RECURRING or NON_RECURRING recurrence modes.
     */
    val billingCycleCount: Int?,

    /**
     * [Price] of the [PricingPhase]
     */
    val price: Price
) : Parcelable {

    /**
     * Indicates how the pricing phase is charged for FINITE_RECURRING pricing phases
     */
    val paymentMode: PaymentMode?
        get() {
            // billingCycleCount is null for INFINITE_RECURRING or NON_RECURRING recurrence modes
            // but validating for FINITE_RECURRING anyway
            if (recurrenceMode != RecurrenceMode.FINITE_RECURRING) {
                return null
            }

            return if (price.amountMicros == 0L) {
                PaymentMode.FREE_TRIAL
            } else if (billingCycleCount == 1) {
                PaymentMode.PAY_UP_FRONT
            } else if (billingCycleCount != null && billingCycleCount > 1) {
                PaymentMode.PAY_AS_YOU_GO
            } else {
                null
            }
        }
}
