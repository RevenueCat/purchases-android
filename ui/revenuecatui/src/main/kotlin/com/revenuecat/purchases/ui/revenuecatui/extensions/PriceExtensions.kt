@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.ui.revenuecatui.data.processed.endsIn00Cents
import com.revenuecat.purchases.ui.revenuecatui.data.processed.getTruncatedFormatted
import java.util.Locale

@JvmSynthetic
internal fun Price.localized(locale: Locale, showZeroDecimalPlacePrices: Boolean): String {
    // always round if rounding on
    return if (showZeroDecimalPlacePrices && this.endsIn00Cents()) {
        this.getTruncatedFormatted(locale)
    } else {
        this.formatted
    }
}

@JvmSynthetic
internal fun Price.localizedPerPeriod(period: Period, locale: Locale, showZeroDecimalPlacePrices: Boolean): String {
    val localizedPrice = this.localized(locale, showZeroDecimalPlacePrices)
    val formattedPeriod = period.localizedAbbreviatedPeriod(locale)
    return "$localizedPrice/$formattedPeriod"
}
