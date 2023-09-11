package com.revenuecat.purchases.paywalls

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.EqMatcher
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.net.URL
import java.util.*

private const val PAYWALLDATA_SAMPLE1 = "paywalldata-sample1.json"

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PaywallDataTest {

    @Before
    fun setUp() {
        mockkConstructor(Locale::class)
    }

    @After
    fun tearDown() {
        unmockkConstructor(Locale::class)
    }

    @Test
    fun `test PaywallData properties`() {
        val json = File(javaClass.classLoader!!.getResource(PAYWALLDATA_SAMPLE1).file).readText()

        val paywall: PaywallData = Json.decodeFromString(json)

        assertThat(paywall.templateName).isEqualTo("1")
        assertThat(paywall.assetBaseURL).isEqualTo(URL("https://rc-paywalls.s3.amazonaws.com"))
        assertThat(paywall.revision).isEqualTo(7)
        assertThat(paywall.config.packages).containsExactly("\$rc_monthly", "\$rc_annual", "custom_package")
        assertThat(paywall.config.defaultPackage).isEqualTo("\$rc_annual")
        assertThat(paywall.config.images.header).isEqualTo("header.jpg")
        assertThat(paywall.config.images.background).isEqualTo("background.jpg")
        assertThat(paywall.config.images.icon).isEqualTo("icon.jpg")
        assertThat(paywall.config.blurredBackgroundImage).isTrue
        assertThat(paywall.config.displayRestorePurchases).isFalse
        assertThat(paywall.config.termsOfServiceURL).isEqualTo(URL("https://revenuecat.com/tos"))
        assertThat(paywall.config.privacyURL).isEqualTo(URL("https://revenuecat.com/privacy"))

        paywall.config.colors.light.let { lightColors ->
            assertThat(lightColors.background.stringRepresentation).isEqualTo("#FF00AA")
            assertThat(lightColors.text1.stringRepresentation).isEqualTo("#FF00AA22")
            assertThat(lightColors.text2?.stringRepresentation).isEqualTo("#FF00AA11")
            assertThat(lightColors.callToActionBackground.stringRepresentation).isEqualTo("#FF00AACC")
            assertThat(lightColors.callToActionForeground.stringRepresentation).isEqualTo("#FF00AA")
            assertThat(lightColors.callToActionSecondaryBackground?.stringRepresentation).isEqualTo("#FF00BB")
            assertThat(lightColors.accent1?.stringRepresentation).isEqualTo("#FF0000")
            assertThat(lightColors.accent2?.stringRepresentation).isEqualTo("#00FF00")
        }

        assertThat(paywall.config.colors.dark).isNotNull
        paywall.config.colors.dark?.let { darkColors ->
            assertThat(darkColors.background.stringRepresentation).isEqualTo("#FF0000")
            assertThat(darkColors.text1.stringRepresentation).isEqualTo("#FF0011")
            assertThat(darkColors.text2).isNull()
            assertThat(darkColors.callToActionBackground.stringRepresentation).isEqualTo("#112233AA")
            assertThat(darkColors.callToActionForeground.stringRepresentation).isEqualTo("#AABBCC")
            assertThat(darkColors.accent1?.stringRepresentation).isEqualTo("#00FFFF")
            assertThat(darkColors.accent2?.stringRepresentation).isEqualTo("#FF00FF")
        }

        mockLocale("en_US", "eng")
        mockLocale("es_ES", "spa")
        mockLocale("gl_ES", "glg")

        val requiredLocale = Locale("gl_ES")
        assertThat(paywall.configForLocale(requiredLocale)).isNull()
    }

    private fun mockLocale(language: String, iso3Code: String) {
        // We need to mock getISO3Language because it's not implemented in Robolectric and it crashes
        every {
            @Suppress("UsePropertyAccessSyntax")
            constructedWith<Locale>(EqMatcher(language)).getISO3Language()
        } returns iso3Code
    }
}
