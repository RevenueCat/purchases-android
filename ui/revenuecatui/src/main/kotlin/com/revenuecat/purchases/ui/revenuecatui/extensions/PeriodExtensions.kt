package com.revenuecat.purchases.ui.revenuecatui.extensions

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.os.Build
import androidx.annotation.RequiresApi
import com.revenuecat.purchases.models.Period
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.N)
internal fun Period.localizedPeriod(locale: Locale): String {
    return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT).format(
        Measure(value, unit.measureUnit),
    )
}

private val Period.Unit.measureUnit: MeasureUnit?
    @RequiresApi(Build.VERSION_CODES.N)
    get() = when (this) {
        Period.Unit.DAY -> MeasureUnit.DAY
        Period.Unit.WEEK -> MeasureUnit.WEEK
        Period.Unit.MONTH -> MeasureUnit.MONTH
        Period.Unit.YEAR -> MeasureUnit.YEAR
        Period.Unit.UNKNOWN -> null
    }
