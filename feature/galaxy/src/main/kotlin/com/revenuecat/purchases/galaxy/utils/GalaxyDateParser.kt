package com.revenuecat.purchases.galaxy.utils

import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal fun String.parseDateFromGalaxyDateString(): Date {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
        timeZone = TimeZone.getDefault()
    }

    return try {
        formatter.parse(this) ?: throw ParseException("Unable to parse date", 0)
    } catch (exception: ParseException) {
        val errorMessage = GalaxyStrings.ERROR_CANNOT_PARSE_PURCHASE_DATE.format(this)
        log(LogIntent.GALAXY_ERROR) { errorMessage }
        throw IllegalArgumentException(errorMessage, exception)
    }
}
