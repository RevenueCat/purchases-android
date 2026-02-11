package com.revenuecat.purchases.common

internal class FakeLocaleProvider(
    vararg languageTags: String,
): LocaleProvider {

    public var languageTags: List<String> = languageTags.toList()

    override val currentLocalesLanguageTags: String
        get() = languageTags.joinToString()
}
