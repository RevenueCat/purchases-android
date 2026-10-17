package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.extensions.localized
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale

private const val MICROS_PER_UNIT = 1_000_000L
private const val DEFAULT_FRACTION_DIGITS = 2

/**
 * What a subscriber saves over the product's first billing period: a yearly product is measured over its first
 * year, a monthly one over its first month.
 *
 * Offer phases count, in order, for the part of that period they cover, and the regular price is prorated over the
 * rest. The iOS implementation must produce the same numbers.
 */
internal class FirstPeriodSavings private constructor(
    /**
     * Rounded half up to the currency's minor unit.
     */
    val amountMicros: Long,
    val currencyCode: String,
    val relative: Double,
) {

    sealed interface Baseline {
        /**
         * The product's own price for one period.
         */
        object StandardPrice : Baseline

        /**
         * The most expensive per-month price in the tab, scaled to the product's period.
         */
        class MostExpensive(val pricePerMonthMicros: Long?) : Baseline
    }

    companion object {

        /**
         * Null when the product isn't a subscription, the baseline is unknown, or nothing is saved.
         */
        @Suppress("ReturnCount")
        fun create(
            product: StoreProduct,
            offerPhases: List<PricingPhase>,
            baseline: Baseline,
        ): FirstPeriodSavings? {
            val period = product.period ?: return null

            val baselineMicros = when (baseline) {
                is Baseline.StandardPrice -> BigDecimal.valueOf(product.price.amountMicros)
                is Baseline.MostExpensive -> {
                    val pricePerMonthMicros = baseline.pricePerMonthMicros ?: return null
                    BigDecimal.valueOf(pricePerMonthMicros).multiply(BigDecimal.valueOf(period.valueInMonths))
                }
            }

            val cost = firstPeriodCostMicros(
                priceMicros = product.price.amountMicros,
                windowDays = period.valueInDays,
                offerPhases = offerPhases,
            )

            val currencyCode = product.price.currencyCode
            val amountMicros = baselineMicros.subtract(cost).roundedToMinorUnit(currencyCode)
            if (baselineMicros.signum() <= 0 || amountMicros.signum() <= 0) return null

            return FirstPeriodSavings(
                amountMicros = amountMicros.toLong(),
                currencyCode = currencyCode,
                relative = amountMicros.divide(baselineMicros, MathContext.DECIMAL64).toDouble(),
            )
        }

        private fun firstPeriodCostMicros(
            priceMicros: Long,
            windowDays: Double,
            offerPhases: List<PricingPhase>,
        ): BigDecimal {
            val window = BigDecimal.valueOf(windowDays)
            if (window.signum() <= 0) return BigDecimal.valueOf(priceMicros)

            var remaining = window
            var cost = BigDecimal.ZERO
            for (phase in offerPhases) {
                val cycles = BigDecimal.valueOf((phase.billingCycleCount ?: 1).toLong())
                val phaseDays = BigDecimal.valueOf(phase.billingPeriod.valueInDays).multiply(cycles)
                if (remaining.signum() > 0 && phaseDays.signum() > 0) {
                    val phaseCost = BigDecimal.valueOf(phase.price.amountMicros).multiply(cycles)
                    val covered = phaseDays.min(remaining)
                    cost += phaseCost.multiply(covered).divide(phaseDays, MathContext.DECIMAL64)
                    remaining -= covered
                }
            }

            return cost + BigDecimal.valueOf(priceMicros).multiply(remaining).divide(window, MathContext.DECIMAL64)
        }

        private fun BigDecimal.roundedToMinorUnit(currencyCode: String): BigDecimal {
            val fractionDigits = runCatching { Currency.getInstance(currencyCode).defaultFractionDigits }
                .getOrNull()
                ?.takeIf { it >= 0 }
                ?: DEFAULT_FRACTION_DIGITS
            val micros = BigDecimal.valueOf(MICROS_PER_UNIT)
            return divide(micros, MathContext.DECIMAL64)
                .setScale(fractionDigits, RoundingMode.HALF_UP)
                .multiply(micros)
        }
    }
}

internal fun Package.standardSavings(mostExpensivePricePerMonthMicros: Long?): FirstPeriodSavings? =
    FirstPeriodSavings.create(
        product = product,
        offerPhases = emptyList(),
        baseline = FirstPeriodSavings.Baseline.MostExpensive(mostExpensivePricePerMonthMicros),
    )

/**
 * Null when the package has no offer.
 */
internal fun Package.offerSavings(
    subscriptionOption: SubscriptionOption?,
    baseline: FirstPeriodSavings.Baseline,
): FirstPeriodSavings? =
    discountPhases(subscriptionOption, this)
        .takeIf { it.isNotEmpty() }
        ?.let { FirstPeriodSavings.create(product, it, baseline) }

internal fun FirstPeriodSavings.localizedAmount(locale: Locale, showZeroDecimalPlacePrices: Boolean): String =
    Price(formatted = "", amountMicros = amountMicros, currencyCode = currencyCode)
        .localized(locale, showZeroDecimalPlacePrices)
