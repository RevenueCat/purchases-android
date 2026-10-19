package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
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
 * What a subscriber saves against a baseline, over one of two windows:
 * - [firstPeriod]: the product's first billing period. A yearly product is measured over its first year, a monthly
 *   one over its first month. Offer phases count, in order, for the part of that period they cover, and the regular
 *   price is prorated over the rest.
 * - [offerTerm]: the offer's full term, against the most expensive per-month price.
 *
 * The iOS implementation must produce the same numbers.
 */
internal class DiscountSavings private constructor(
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
        fun firstPeriod(
            product: StoreProduct,
            offerPhases: List<PricingPhase>,
            baseline: Baseline,
        ): DiscountSavings? {
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

            return create(product, baselineMicros, cost)
        }

        /**
         * Without a paid offer phase, the term is one period of the product at its regular price. A free trial
         * counts as no offer, since "100% off" isn't the claim this makes.
         *
         * Null when the product isn't a subscription, the most expensive price is unknown, the offer's length is
         * indeterminate, or nothing is saved.
         */
        @Suppress("ReturnCount")
        fun offerTerm(
            product: StoreProduct,
            offerPhases: List<PricingPhase>,
            mostExpensivePricePerMonthMicros: Long?,
        ): DiscountSavings? {
            val pricePerMonthMicros = mostExpensivePricePerMonthMicros ?: return null
            val period = product.period ?: return null

            val offerPhase = offerPhases.firstOrNull { it.price.amountMicros > 0 }
            val months: BigDecimal
            val cost: BigDecimal
            if (offerPhase != null) {
                val cycles = BigDecimal.valueOf(offerPhase.cycleCount()?.toLong() ?: return null)
                months = BigDecimal.valueOf(offerPhase.billingPeriod.valueInMonths).multiply(cycles)
                cost = BigDecimal.valueOf(offerPhase.price.amountMicros).multiply(cycles)
            } else {
                months = BigDecimal.valueOf(period.valueInMonths)
                cost = BigDecimal.valueOf(product.price.amountMicros)
            }

            return create(product, BigDecimal.valueOf(pricePerMonthMicros).multiply(months), cost)
        }

        private fun create(product: StoreProduct, baselineMicros: BigDecimal, cost: BigDecimal): DiscountSavings? {
            val currencyCode = product.price.currencyCode
            val amountMicros = baselineMicros.subtract(cost).roundedToMinorUnit(currencyCode)
            if (baselineMicros.signum() <= 0 || amountMicros.signum() <= 0) return null

            return DiscountSavings(
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
                val cycles = BigDecimal.valueOf(phase.cycleCount()?.toLong() ?: 1L)
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

        /**
         * Null when the phase repeats until cancellation. Play reports 0 cycles, not null, for a single payment
         * phase, so that counts as one.
         */
        private fun PricingPhase.cycleCount(): Int? = when (recurrenceMode) {
            RecurrenceMode.INFINITE_RECURRING -> null
            RecurrenceMode.NON_RECURRING -> 1
            RecurrenceMode.FINITE_RECURRING,
            RecurrenceMode.UNKNOWN,
            -> billingCycleCount?.takeIf { it > 0 } ?: 1
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

internal fun Package.standardSavings(mostExpensivePricePerMonthMicros: Long?): DiscountSavings? =
    DiscountSavings.firstPeriod(
        product = product,
        offerPhases = emptyList(),
        baseline = DiscountSavings.Baseline.MostExpensive(mostExpensivePricePerMonthMicros),
    )

/**
 * The offer against the product's own standard price. Null when the package has no offer.
 */
internal fun Package.offerSavings(subscriptionOption: SubscriptionOption?): DiscountSavings? =
    discountPhases(subscriptionOption, this)
        .takeIf { it.isNotEmpty() }
        ?.let { DiscountSavings.firstPeriod(product, it, DiscountSavings.Baseline.StandardPrice) }

/**
 * Without an offer, this matches `product.absolute_discount`.
 */
internal fun Package.fullPeriodSavings(
    subscriptionOption: SubscriptionOption?,
    mostExpensivePricePerMonthMicros: Long?,
): DiscountSavings? =
    DiscountSavings.firstPeriod(
        product = product,
        offerPhases = discountPhases(subscriptionOption, this),
        baseline = DiscountSavings.Baseline.MostExpensive(mostExpensivePricePerMonthMicros),
    )

/**
 * Without a paid offer phase, this matches `product.absolute_discount`.
 */
internal fun Package.offerTermSavings(
    subscriptionOption: SubscriptionOption?,
    mostExpensivePricePerMonthMicros: Long?,
): DiscountSavings? =
    DiscountSavings.offerTerm(product, discountPhases(subscriptionOption, this), mostExpensivePricePerMonthMicros)

internal fun DiscountSavings.localizedAmount(locale: Locale, showZeroDecimalPlacePrices: Boolean): String =
    Price(formatted = "", amountMicros = amountMicros, currencyCode = currencyCode)
        .localized(locale, showZeroDecimalPlacePrices)
