package com.revenuecat.purchases.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents subscription or pricing phase billing period
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
    val iso8601: String
) : Parcelable {
    @SuppressWarnings("MagicNumber")
    enum class Unit(val identifier: Int) {
        DAY(0),
        WEEK(1),
        MONTH(2),
        YEAR(3),
        UNKNOWN(4)
    }
}

// Would use Duration.parse but only available API 26 and up
fun String.toSubscriptionPeriod(): Period {
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
            Period(yearInt, Period.Unit.YEAR, this)
        } else if (monthInt > 0) {
            Period(monthInt, Period.Unit.MONTH, this)
        } else if (weekInt > 0) {
            Period(weekInt, Period.Unit.WEEK, this)
        } else if (dayInt > 0) {
            Period(dayInt, Period.Unit.DAY, this)
        } else {
            Period(0, Period.Unit.UNKNOWN, this)
        }
    }

    return Period(0, Period.Unit.UNKNOWN, this)
}
