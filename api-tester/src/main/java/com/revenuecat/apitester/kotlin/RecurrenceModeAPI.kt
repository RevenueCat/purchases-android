package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.RecurrenceMode

@Suppress("unused", "UNUSED_VARIABLE")
private class RecurrenceModeAPI {
    fun check(recurrenceMode: RecurrenceMode) {
        when (recurrenceMode) {
            RecurrenceMode.INFINITE_RECURRING,
            RecurrenceMode.FINITE_RECURRING,
            RecurrenceMode.NON_RECURRING,
            RecurrenceMode.UNKNOWN,
            -> {
            }
        }.exhaustive
    }
}
