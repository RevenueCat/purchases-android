package com.revenuecat.purchases.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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

        return if (yearInt > 0) {
            Pair(yearInt, Period.Unit.YEAR)
        } else if (monthInt > 0) {
            Pair(monthInt, Period.Unit.MONTH)
        } else if (weekInt > 0) {
            Pair(weekInt, Period.Unit.WEEK)
        } else if (dayInt > 0) {
            Pair(dayInt, Period.Unit.DAY)
        } else {
            Pair(0, Period.Unit.UNKNOWN)
        }
    }

    return Pair(0, Period.Unit.UNKNOWN)
}
