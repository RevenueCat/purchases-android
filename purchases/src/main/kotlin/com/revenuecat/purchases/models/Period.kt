package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.common.errorLog
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt

private object PeriodConstants {
    const val DAYS_PER_WEEK = 7.0
    const val DAYS_PER_MONTH = 30.0
    const val DAYS_PER_YEAR = 365.0
    const val WEEKS_PER_YEAR = DAYS_PER_YEAR / DAYS_PER_WEEK
    const val MONTHS_PER_YEAR = 12.0
    const val WEEKS_PER_MONTH = DAYS_PER_YEAR / MONTHS_PER_YEAR / DAYS_PER_WEEK
}

/**
 * Represents subscription or [PricingPhase] billing period
 */
@Parcelize
data class Period(
    /**
     * The number of period units.
     */
    val value: Int,

    /**
     * The increment of time that a subscription period is specified in.
     */
    val unit: Unit,

    /**
     * Specified in ISO 8601 format. For example, P1W equates to one week,
     * P1M equates to one month, P3M equates to three months, P6M equates to six months,
     * and P1Y equates to one year
     */
    val iso8601: String,
) : Parcelable {

    companion object Factory {
        /**
         * Creates a [Period] object from an ISO 8601 string. Supports both ISO 8601-1 and ISO 8601-2 formats.
         * You shouldn't normally need to call this method directly since `Period` objects are created by the SDK.
         * This can be useful in some cases for testing purposes.
         *
         * @param iso8601 The ISO 8601 string to parse
         */
        fun create(iso8601: String): Period {
            val pair = iso8601.toPeriod()
            return Period(pair.first, pair.second, iso8601)
        }
    }

    @SuppressWarnings("MagicNumber")
    enum class Unit {
        DAY,
        WEEK,
        MONTH,
        YEAR,
        UNKNOWN,
    }

    /**
     * The period value in week units. This is an approximated value.
     */
    internal val valueInWeeks: Double
        get() = when (unit) {
            Unit.DAY -> value / PeriodConstants.DAYS_PER_WEEK
            Unit.WEEK -> value.toDouble()
            Unit.MONTH -> value.toDouble() * PeriodConstants.WEEKS_PER_MONTH
            Unit.YEAR -> value * PeriodConstants.WEEKS_PER_YEAR
            Unit.UNKNOWN -> {
                errorLog("Unknown period unit trying to get value in months: $unit")
                0.0
            }
        }

    /**
     * The period value in month units. This is an approximated value.
     */
    val valueInMonths: Double
        get() = when (unit) {
            Unit.DAY -> value / PeriodConstants.DAYS_PER_MONTH
            Unit.WEEK -> value / PeriodConstants.WEEKS_PER_MONTH
            Unit.MONTH -> value.toDouble()
            Unit.YEAR -> value * PeriodConstants.MONTHS_PER_YEAR
            Unit.UNKNOWN -> {
                errorLog("Unknown period unit trying to get value in months: $unit")
                0.0
            }
        }

    /**
     * The period value in week units. This is an approximated value.
     */
    internal val valueInYears: Double
        get() = when (unit) {
            Unit.DAY -> value / PeriodConstants.DAYS_PER_YEAR
            Unit.WEEK -> value / PeriodConstants.WEEKS_PER_YEAR
            Unit.MONTH -> value / PeriodConstants.MONTHS_PER_YEAR
            Unit.YEAR -> value.toDouble()
            Unit.UNKNOWN -> {
                errorLog("Unknown period unit trying to get value in months: $unit")
                0.0
            }
        }
}

// Would use Duration.parse but only available API 26 and up
private fun String.toPeriod(): Pair<Int, Period.Unit> {
    // Takes from https://stackoverflow.com/a/32045167
    val regex = "^P(?!\$)(\\d+(?:\\.\\d+)?Y)?(\\d+(?:\\.\\d+)?M)?(\\d+(?:\\.\\d+)?W)?(\\d+(?:\\.\\d+)?D)?\$"
        .toRegex()
        .matchEntire(this)

    regex?.let { periodResult ->
        val toInt = fun(part: String): Int {
            return part.dropLast(1).toIntOrNull() ?: 0
        }

        val (year, month, week, day) = periodResult.destructured

        val yearInt = toInt(year)
        val monthInt = toInt(month)
        val weekInt = toInt(week)
        val dayInt = toInt(day)

        val smallerUnit = when {
            dayInt > 0 -> Period.Unit.DAY
            weekInt > 0 -> Period.Unit.WEEK
            monthInt > 0 -> Period.Unit.MONTH
            yearInt > 0 -> Period.Unit.YEAR
            else -> Period.Unit.UNKNOWN
        }

        val value = when (smallerUnit) {
            Period.Unit.YEAR -> yearInt.toDouble()
            Period.Unit.MONTH -> (yearInt * PeriodConstants.MONTHS_PER_YEAR) +
                monthInt
            Period.Unit.WEEK -> (yearInt * PeriodConstants.WEEKS_PER_YEAR) +
                (monthInt * PeriodConstants.WEEKS_PER_MONTH) +
                weekInt
            Period.Unit.DAY -> (yearInt * PeriodConstants.DAYS_PER_YEAR) +
                (monthInt * PeriodConstants.DAYS_PER_MONTH) +
                (weekInt * PeriodConstants.DAYS_PER_WEEK) +
                dayInt
            Period.Unit.UNKNOWN -> 0.0
        }

        return Pair(value.roundToInt(), smallerUnit)
    }

    return Pair(0, Period.Unit.UNKNOWN)
}
