package com.revenuecat.purchases.ui.revenuecatui.extensions

import android.content.res.Resources
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import kotlin.math.roundToInt

@SuppressWarnings("MagicNumber")
internal fun TemplateConfiguration.PackageInfo.localizedDiscount(
    resources: Resources,
): String {
    return (discountRelativeToMostExpensivePerMonth?.times(100.0))?.roundToInt()?.let {
        resources.getString(R.string.package_discount, it)
    } ?: ""
}
