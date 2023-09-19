package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.Period

@Suppress("unused", "UNUSED_VARIABLE")
private class PeriodAPI {
    fun check(period: Period) {
        with(period) {
            val value: Int = value
            val unit: Period.Unit = unit
            val iso8601: String = iso8601
            val valueInMonths: Double = valueInMonths

            when (unit) {
                Period.Unit.DAY,
                Period.Unit.WEEK,
                Period.Unit.MONTH,
                Period.Unit.YEAR,
                Period.Unit.UNKNOWN,
                -> {
                }
            }.exhaustive
        }
    }
}
