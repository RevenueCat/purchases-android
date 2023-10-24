package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.helpers.ApplicationContext
import kotlin.math.roundToInt

@SuppressWarnings("MagicNumber")
internal fun TemplateConfiguration.PackageInfo.localizedDiscount(
    applicationContext: ApplicationContext,
): String {
    return (discountRelativeToMostExpensivePerMonth?.times(100.0))?.roundToInt()?.let {
        applicationContext.getString(R.string.package_discount, it)
    } ?: ""
}
