package com.revenuecat.purchases.ui.revenuecatui.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

internal interface DateFormatter {

    fun format(date: Date, locale: Locale): String
}

internal class DefaultDateFormatter : DateFormatter {

    override fun format(date: Date, locale: Locale): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            formatUsingDateTimeFormatter(date, locale)
        } else {
            formatUsingSimpleDateFormat(date, locale)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatUsingDateTimeFormatter(date: Date, locale: Locale): String {
        val localDate = date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", locale)
        return localDate.format(formatter)
    }

    private fun formatUsingSimpleDateFormat(date: Date, locale: Locale): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", locale)
        return formatter.format(date)
    }
}
