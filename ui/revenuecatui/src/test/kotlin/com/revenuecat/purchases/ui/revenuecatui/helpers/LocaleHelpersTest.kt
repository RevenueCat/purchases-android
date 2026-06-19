package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
internal class LocaleHelpersTest {

    @Test
    fun `toLayoutDirection returns RTL for Hebrew`() {
        assertThat(Locale.forLanguageTag("he-IL").toLayoutDirection()).isEqualTo(LayoutDirection.Rtl)
    }

    @Test
    fun `toLayoutDirection returns RTL for Arabic`() {
        assertThat(Locale.forLanguageTag("ar").toLayoutDirection()).isEqualTo(LayoutDirection.Rtl)
    }

    @Test
    fun `toLayoutDirection returns RTL for Farsi`() {
        assertThat(Locale.forLanguageTag("fa").toLayoutDirection()).isEqualTo(LayoutDirection.Rtl)
    }

    @Test
    fun `toLayoutDirection returns LTR for English`() {
        assertThat(Locale.US.toLayoutDirection()).isEqualTo(LayoutDirection.Ltr)
    }

    @Test
    fun `toLayoutDirection returns LTR for unknown locale`() {
        assertThat(Locale.ROOT.toLayoutDirection()).isEqualTo(LayoutDirection.Ltr)
    }
}
