package com.revenuecat.purchases.utils

import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

internal data class DateActive(val isActive: Boolean, val inGracePeriod: Boolean)

internal class DateHelper private constructor() {
    companion object {
        private val ENTITLEMENT_GRACE_PERIOD = 3.days

        internal val now: Date get() = Date()

        /**
         * Calculates whether a subscription/entitlement is currently active according to the expiration date and last
         * successful request date, while considering a given grace period
         */
        fun isDateActive(
            expirationDate: Date?,
            requestDate: Date,
            gracePeriod: Duration = ENTITLEMENT_GRACE_PERIOD,
        ): DateActive {
            if (expirationDate == null) return DateActive(isActive = true, inGracePeriod = true)

            val currentDate = now
            val inGracePeriod = (currentDate.time - requestDate.time) <= gracePeriod.inWholeMilliseconds
            val referenceDate = if (inGracePeriod) requestDate else currentDate
            return DateActive(isActive = expirationDate.after(referenceDate), inGracePeriod = inGracePeriod)
        }
    }
}
