@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import android.os.Build
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Config
import com.revenuecat.purchases.common.LocaleProvider
import com.revenuecat.purchases.common.platformName
import java.util.Date

/**
 * Device and environment dimensions.
 *
 * Everything but the locale is fixed for the life of the process and is read once. The locale is read per
 * evaluation because the system locale can change mid-session and `preferredUILocaleOverride` can be set at any
 * time, and an audience keyed on locale has to agree with what the customer will actually be shown.
 *
 * A dimension the device has no value for is omitted; see [hasValue].
 */
internal class DeviceDimensionProvider(
    appConfig: AppConfig,
    private val localeProvider: LocaleProvider,
) : RulesDimensionProvider {

    override val name: String = "device"

    private val fixedDimensions: Map<String, RulesDimensionValue> = mapOf(
        KEY_APP_VERSION to RulesDimensionValue.StringValue(appConfig.versionName),
        KEY_PLATFORM to RulesDimensionValue.StringValue(appConfig.store.platformName),
        // A string rather than a number so one predicate can treat the platform version the same on every
        // platform: iOS reports versions like "26.1", which only a string can carry.
        KEY_PLATFORM_VERSION to RulesDimensionValue.StringValue(Build.VERSION.SDK_INT.toString()),
        KEY_SDK_VERSION to RulesDimensionValue.StringValue(Config.frameworkVersion),
    )

    // Snapshotted at configure time, so it is the fallback for the rare case where the live preference list is
    // empty.
    private val fallbackLanguageTag: String = appConfig.languageTag

    override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> =
        (fixedDimensions + (KEY_LOCALE to RulesDimensionValue.StringValue(currentLocale())))
            .filterValues { value -> value.hasValue }

    /**
     * [LocaleProvider.currentLocalesLanguageTags] is the comma-joined preference list sent as a header. A
     * dimension has to be a scalar to be usable with `==` or `in`, so only the preferred locale is exposed.
     *
     * Normalized to lowercase snake case (`en_us`) so one predicate can target a locale on every platform,
     * whatever casing and separator that platform's locale API happens to use.
     */
    private fun currentLocale(): String =
        localeProvider.currentLocalesLanguageTags
            .substringBefore(',')
            .ifEmpty { fallbackLanguageTag }
            .lowercase()
            .replace(oldChar = '-', newChar = '_')

    internal companion object {
        const val KEY_APP_VERSION = "app_version"
        const val KEY_LOCALE = "locale"
        const val KEY_PLATFORM = "platform"
        const val KEY_PLATFORM_VERSION = "platform_version"
        const val KEY_SDK_VERSION = "sdk_version"

        /**
         * A dimension the device could not tell us about is omitted rather than reported as an empty string: a
         * predicate that reads an absent key fails as an unresolved variable, while `""` would quietly compare
         * equal to another device's missing value.
         */
        private val RulesDimensionValue.hasValue: Boolean
            get() = this !is RulesDimensionValue.StringValue || value.isNotEmpty()
    }
}
