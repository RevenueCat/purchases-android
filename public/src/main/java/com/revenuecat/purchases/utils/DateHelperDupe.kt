package com.revenuecat.purchases.utils

import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * TODO this is a dupe of DateHelper.kt in common module, but it's used in the public module so we can't remove it yet,
 * nor make it public here because it would be accessible to everyone
 */
internal data class DateActiveDupe(val isActive: Boolean, val inGracePeriod: Boolean)

internal class DateHelperDupe private constructor() {
    companion object {
        private val ENTITLEMENT_GRACE_PERIOD = 3.days

        /**
         * Calculates whether a subscription/entitlement is currently active according to the expiration date and last
         * successful request date, while considering a given grace period
         */
        fun isDateActive(
            expirationDate: Date?,
            requestDate: Date,
            gracePeriod: Duration = ENTITLEMENT_GRACE_PERIOD,
        ): DateActiveDupe {
            if (expirationDate == null) return DateActiveDupe(isActive = true, inGracePeriod = true)

            val inGracePeriod = (Date().time - requestDate.time) <= gracePeriod.inWholeMilliseconds
            val referenceDate = if (inGracePeriod) requestDate else Date()
            return DateActiveDupe(isActive = expirationDate.after(referenceDate), inGracePeriod = inGracePeriod)
        }
    }
}
