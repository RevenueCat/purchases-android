package com.revenuecat.purchases.common

import androidx.core.os.LocaleListCompat

internal interface LocaleProvider {
    val currentLocalesLanguageTags: String
}

internal class DefaultLocaleProvider : LocaleProvider {

    private var preferredLocaleOverride: String? = null

    fun setPreferredLocaleOverride(localeString: String?) {
        preferredLocaleOverride = localeString
    }

    override val currentLocalesLanguageTags: String
        get() {
            val result = preferredLocaleOverride?.let {
                val defaultLocales = LocaleListCompat.getDefault().toLanguageTags()
                if (defaultLocales.isEmpty()) {
                    it
                } else {
                    "$it,$defaultLocales"
                }
            } ?: LocaleListCompat.getDefault().toLanguageTags()

            return result
        }
}
