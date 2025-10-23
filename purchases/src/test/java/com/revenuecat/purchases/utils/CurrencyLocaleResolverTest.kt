package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class CurrencyLocaleResolverTest {

    @Test
    fun `when storefront country matches device locale country, returns device locale`() {
        val deviceLocale = Locale("en", "US")
        val result = CurrencyLocaleResolver.resolve(
            storefrontCountryCode = "US",
            locale = deviceLocale,
        )

        assertThat(result.language).isEqualTo("en")
        assertThat(result.country).isEqualTo("US")
    }

    @Test
    fun `when storefront country differs but language matches, returns locale with storefront country and device language`() {
        // Device is set to en-US, but storefront is NL (Netherlands)
        val deviceLocale = Locale("en", "US")
        val result = CurrencyLocaleResolver.resolve(
            storefrontCountryCode = "NL",
            locale = deviceLocale,
        )

        // Should find en_NL in available locales (if it exists on the device)
        // or create a locale with en language and NL country
        assertThat(result.language).isEqualTo("en")
        assertThat(result.country).isEqualTo("NL")
    }

    @Test
    fun `when storefront country is DE and device is en-US, returns en-DE`() {
        val deviceLocale = Locale("en", "US")
        val result = CurrencyLocaleResolver.resolve(
            storefrontCountryCode = "DE",
            locale = deviceLocale,
        )

        assertThat(result.language).isEqualTo("en")
        assertThat(result.country).isEqualTo("DE")
    }

    @Test
    fun `when device locale is Spanish Mexico but storefront is Spain, returns es-ES`() {
        val deviceLocale = Locale("es", "MX")
        val result = CurrencyLocaleResolver.resolve(
            storefrontCountryCode = "ES",
            locale = deviceLocale,
        )

        // Should find es_ES in available locales (Spanish Spain)
        assertThat(result.language).isEqualTo("es")
        assertThat(result.country).isEqualTo("ES")
    }

    @Test
    fun `when storefront country is null, returns device locale unchanged`() {
        val deviceLocale = Locale("nl", "NL")
        val result = CurrencyLocaleResolver.resolve(
            storefrontCountryCode = null,
            locale = deviceLocale,
        )

        assertThat(result).isEqualTo(deviceLocale)
        assertThat(result.language).isEqualTo("nl")
        assertThat(result.country).isEqualTo("NL")
    }

    @Test
    fun `when storefront country is empty string, returns device locale unchanged`() {
        val deviceLocale = Locale("fr", "FR")
        val result = CurrencyLocaleResolver.resolve(
            storefrontCountryCode = "",
            locale = deviceLocale,
        )

        assertThat(result).isEqualTo(deviceLocale)
        assertThat(result.language).isEqualTo("fr")
        assertThat(result.country).isEqualTo("FR")
    }

    @Test
    fun `when storefront country is blank, returns device locale unchanged`() {
        val deviceLocale = Locale("de", "DE")
        val result = CurrencyLocaleResolver.resolve(
            storefrontCountryCode = "  ",
            locale = deviceLocale,
        )

        assertThat(result).isEqualTo(deviceLocale)
        assertThat(result.language).isEqualTo("de")
        assertThat(result.country).isEqualTo("DE")
    }

    @Test
    fun `storefront country takes precedence over device locale country for currency determination`() {
        // Device is in Mexico (MXN currency), but storefront is US (USD currency)
        val deviceLocale = Locale("es", "MX")
        val result = CurrencyLocaleResolver.resolve(
            storefrontCountryCode = "US",
            locale = deviceLocale,
        )

        // Country should be US (which determines currency as USD)
        assertThat(result.country).isEqualTo("US")
        // Language might be es if es_US exists in available locales, or en otherwise
        // The important part is the country is correct for currency determination
    }

    @Test
    fun `when device locale is French Canada but storefront is Switzerland, returns locale with CH country`() {
        val deviceLocale = Locale("fr", "CA")
        val result = CurrencyLocaleResolver.resolve(
            storefrontCountryCode = "CH",
            locale = deviceLocale,
        )

        // Should create/find fr_CH (French Switzerland)
        assertThat(result.language).isEqualTo("fr")
        assertThat(result.country).isEqualTo("CH")
    }

    @Test
    fun `when storefront country is lowercase, it still works correctly`() {
        val deviceLocale = Locale("en", "US")
        val result = CurrencyLocaleResolver.resolve(
            storefrontCountryCode = "gb",
            locale = deviceLocale,
        )

        // Should handle lowercase and return en_GB
        assertThat(result.language).isEqualTo("en")
        assertThat(result.country).isEqualTo("GB")
    }

    @Test
    fun `when device has no country code, storefront country is used`() {
        val deviceLocale = Locale("en") // No country specified
        val result = CurrencyLocaleResolver.resolve(
            storefrontCountryCode = "AU",
            locale = deviceLocale,
        )

        assertThat(result.language).isEqualTo("en")
        assertThat(result.country).isEqualTo("AU")
    }

    @Test
    fun `when no locale parameter provided, uses system default`() {
        // This test verifies the default parameter works
        val result = CurrencyLocaleResolver.resolve(
            storefrontCountryCode = "JP",
        )

        // Should use Locale.getDefault() and apply JP country
        assertThat(result.country).isEqualTo("JP")
    }
}
