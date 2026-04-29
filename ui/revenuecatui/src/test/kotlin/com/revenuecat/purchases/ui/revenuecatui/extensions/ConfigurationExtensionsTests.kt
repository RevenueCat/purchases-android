package com.revenuecat.purchases.ui.revenuecatui.extensions

import android.content.res.Configuration
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsLayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
internal class ConfigurationExtensionsTests {

    @Test
    fun `rcLayoutDirection returns Ltr for English`() {
        val configuration = Configuration().apply {
            setLocale(Locale.forLanguageTag("en-US"))
        }

        assertEquals(LayoutDirection.Ltr, configuration.rcLayoutDirection())
    }

    @Test
    fun `rcLayoutDirection returns Rtl for Hebrew`() {
        val configuration = Configuration().apply {
            setLocale(Locale.forLanguageTag("he"))
        }

        assertEquals(LayoutDirection.Rtl, configuration.rcLayoutDirection())
    }

    @Test
    fun `rcLayoutDirection returns Rtl for Saudi Arabic`() {
        val configuration = Configuration().apply {
            setLocale(Locale.forLanguageTag("ar-SA"))
        }

        assertEquals(LayoutDirection.Rtl, configuration.rcLayoutDirection())
    }

    @Test
    fun `resolveLayoutDirection does not override by default`() {
        val configuration = Configuration().apply {
            setLocale(Locale.forLanguageTag("he"))
        }

        assertNull(configuration.resolveLayoutDirection(null, honorPreferredLocaleLayoutDirection = false))
    }

    @Test
    fun `resolveLayoutDirection honors locale when opted in`() {
        val configuration = Configuration().apply {
            setLocale(Locale.forLanguageTag("he"))
        }

        assertEquals(
            LayoutDirection.Rtl,
            configuration.resolveLayoutDirection(null, honorPreferredLocaleLayoutDirection = true),
        )
    }

    @Test
    fun `resolveLayoutDirection editor setting wins over SDK opt in`() {
        val configuration = Configuration().apply {
            setLocale(Locale.forLanguageTag("he"))
        }

        assertEquals(
            LayoutDirection.Ltr,
            configuration.resolveLayoutDirection(
                PaywallComponentsLayoutDirection.LTR,
                honorPreferredLocaleLayoutDirection = true,
            ),
        )
        assertEquals(
            LayoutDirection.Rtl,
            configuration.resolveLayoutDirection(
                PaywallComponentsLayoutDirection.RTL,
                honorPreferredLocaleLayoutDirection = false,
            ),
        )
    }
}
