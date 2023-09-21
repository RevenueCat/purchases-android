package com.revenuecat.purchases.ui.revenuecatui.extensions

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import com.revenuecat.purchases.models.Period
import java.util.Locale

internal fun Period.localizedAbbreviatedPeriod(
    locale: Locale,
): String {
    var formattedPeriod = localizedPeriod(locale, MeasureFormat.FormatWidth.SHORT)
    if (value == 1 && formattedPeriod.startsWith("1")) {
        formattedPeriod = formattedPeriod.substring(startIndex = 1).trim()
    }
    return formattedPeriod
}

internal fun Period.localizedPeriod(
    locale: Locale,
    formatWidth: MeasureFormat.FormatWidth = MeasureFormat.FormatWidth.WIDE,
): String {
    return MeasureFormat.getInstance(locale, formatWidth).format(
        Measure(value, unit.measureUnit),
    )
}

private val Period.Unit.measureUnit: MeasureUnit?
    get() = when (this) {
        Period.Unit.DAY -> MeasureUnit.DAY
        Period.Unit.WEEK -> MeasureUnit.WEEK
        Period.Unit.MONTH -> MeasureUnit.MONTH
        Period.Unit.YEAR -> MeasureUnit.YEAR
        Period.Unit.UNKNOWN -> null
    }
