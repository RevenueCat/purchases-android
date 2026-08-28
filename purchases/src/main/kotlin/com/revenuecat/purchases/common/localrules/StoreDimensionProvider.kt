@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.warnLog
import kotlinx.coroutines.CancellationException
import java.util.Date
import java.util.IllformedLocaleException
import java.util.Locale
import java.util.MissingResourceException

/**
 * Store dimensions: what the customer's storefront says about where they buy.
 *
 * `country` is an ISO 3166-1 **alpha-3** code such as `USA`, which is the format StoreKit hands the iOS SDK, so one
 * predicate can target a country on either platform. Android's stores report alpha-2 (`US`), so it is converted
 * here, and a code with no alpha-3 equivalent is omitted rather than guessed — the Amazon Appstore reports a
 * marketplace rather than a country, and values like `UK` are not ISO regions.
 */
internal class StoreDimensionProvider(
    private val storefrontCountryCode: suspend () -> String?,
) : RulesDimensionProvider {

    override val namespace: RulesDimensionNamespace = RulesDimensionNamespace.Store

    /**
     * Fetched rather than snapshotted: the storefront is not known until the store has been asked, and it can
     * change while the app is running.
     *
     * A store that cannot answer contributes no dimensions instead of failing the snapshot, whatever the reason:
     * the Galaxy Store never answers, any store can fail transiently, and the fetch reaches out through the
     * configured instance, which an app can tear down mid-evaluation. None of that should abort the snapshot and
     * take the other dimensions — and an otherwise resolvable checkpoint — down with it. Cancellation is not a
     * failure and propagates.
     */
    @Suppress("ReturnCount")
    override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> {
        val countryCode = try {
            storefrontCountryCode()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            warnLog { "The store country is unavailable, so store dimensions can't be evaluated: $e" }
            return emptyMap()
        }
        if (countryCode.isNullOrEmpty()) return emptyMap()

        val alpha3CountryCode = countryCode.asAlpha3CountryCodeOrNull() ?: run {
            warnLog { "Ignoring store country '$countryCode': it has no ISO 3166-1 alpha-3 equivalent." }
            return emptyMap()
        }
        return mapOf(KEY_COUNTRY to RulesDimensionValue.StringValue(alpha3CountryCode))
    }

    private fun String.asAlpha3CountryCodeOrNull(): String? =
        try {
            Locale.Builder().setRegion(this).build().isO3Country.ifEmpty { null }
        } catch (_: IllformedLocaleException) {
            null
        } catch (_: MissingResourceException) {
            null
        }

    internal companion object {
        const val KEY_COUNTRY = "country"
    }
}
