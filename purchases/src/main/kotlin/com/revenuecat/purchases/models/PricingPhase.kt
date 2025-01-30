package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.utils.pricePerDay
import com.revenuecat.purchases.utils.pricePerMonth
import com.revenuecat.purchases.utils.pricePerWeek
import com.revenuecat.purchases.utils.pricePerYear
import kotlinx.parcelize.Parcelize
import java.util.Locale

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
    val price: Price,
) : Parcelable {

    /**
     * Indicates how the pricing phase is charged for FINITE_RECURRING pricing phases
     */
    val offerPaymentMode: OfferPaymentMode?
        get() {
            // billingCycleCount is null for INFINITE_RECURRING or NON_RECURRING recurrence modes
            // but validating for FINITE_RECURRING anyway
            if (recurrenceMode != RecurrenceMode.FINITE_RECURRING) {
                return null
            }

            return if (price.amountMicros == 0L) {
                OfferPaymentMode.FREE_TRIAL
            } else if (billingCycleCount == 1) {
                OfferPaymentMode.SINGLE_PAYMENT
            } else if (billingCycleCount != null && billingCycleCount > 1) {
                OfferPaymentMode.DISCOUNTED_RECURRING_PAYMENT
            } else {
                null
            }
        }

    /**
     * Gives the price of the [PricingPhase] in the given locale in a daily recurrence. This means that for example,
     * if the period is weekly, the price will be divided by 7. It uses a currency formatter to format the price in
     * the given locale. Note that this value may be an approximation.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    @JvmOverloads
    fun pricePerDay(locale: Locale = Locale.getDefault()): Price =
        price.pricePerDay(billingPeriod, locale)

    /**
     * Gives the price of the [PricingPhase] in the given locale in a weekly recurrence. This means that for example,
     * if the period is monthly, the price will be divided by 4. It uses a currency formatter to format the price in
     * the given locale. Note that this value may be an approximation.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    @JvmOverloads
    fun pricePerWeek(locale: Locale = Locale.getDefault()): Price =
        price.pricePerWeek(billingPeriod, locale)

    /**
     * Gives the price of the [PricingPhase] in the given locale in a monthly recurrence. This means that for example,
     * if the period is annual, the price will be divided by 12. It uses a currency formatter to format the price in
     * the given locale. Note that this value may be an approximation.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    @JvmOverloads
    fun pricePerMonth(locale: Locale = Locale.getDefault()): Price =
        price.pricePerMonth(billingPeriod, locale)

    /**
     * Gives the price of the [PricingPhase] in the given locale in a yearly recurrence. This means that for example,
     * if the period is monthly, the price will be multiplied by 12. It uses a currency formatter to format the price in
     * the given locale. Note that this value may be an approximation.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    @JvmOverloads
    fun pricePerYear(locale: Locale = Locale.getDefault()): Price =
        price.pricePerYear(billingPeriod, locale)

    /**
     * Gives the price of the [PricingPhase] in the given locale in a monthly recurrence. This means that for example,
     * if the period is annual, the price will be divided by 12. It uses a currency formatter to format the price in
     * the given locale. Note that this value may be an approximation.
     *
     * This is equivalent to:
     * ```kotlin
     * pricePerMonth(locale).formatted
     * ```
     *
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    @Deprecated(
        message = "pricePerMonth() provides more price info",
        replaceWith = ReplaceWith("pricePerMonth(locale).formatted"),
    )
    @JvmOverloads
    fun formattedPriceInMonths(locale: Locale = Locale.getDefault()): String {
        return pricePerMonth(locale).formatted
    }
}
