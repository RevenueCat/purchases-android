package com.revenuecat.purchases.common

import android.os.LocaleList
import androidx.core.os.LocaleListCompat

internal interface LocaleProvider {
    val currentLocalesLanguageTags: String
}

internal class DefaultLocaleProvider: LocaleProvider {
    override val currentLocalesLanguageTags: String
        get() = LocaleListCompat.getDefault().toLanguageTags()
}
