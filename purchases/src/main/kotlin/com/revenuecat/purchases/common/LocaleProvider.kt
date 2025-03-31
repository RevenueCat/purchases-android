package com.revenuecat.purchases.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.distinct
import com.revenuecat.purchases.utils.plus

internal interface LocaleProvider {
    val currentLocalesLanguageTags: String
}

internal class DefaultLocaleProvider : LocaleProvider {
    @OptIn(InternalRevenueCatAPI::class)
    override val currentLocalesLanguageTags: String
        get() = (AppCompatDelegate.getApplicationLocales() + LocaleListCompat.getDefault()).distinct().toLanguageTags()
}
