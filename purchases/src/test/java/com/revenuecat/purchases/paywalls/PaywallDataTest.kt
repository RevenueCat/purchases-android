package com.revenuecat.purchases.paywalls

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.utils.toLocale
import kotlinx.serialization.decodeFromString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.net.URL
import java.util.*

private const val PAYWALLDATA_SAMPLE1 = "paywalldata-sample1.json"
private const val PAYWALLDATA_MISSING_CURRENT_LOCALE = "paywalldata-missing_current_locale.json"
private const val PAYWALLDATA_EMPTY_IMAGES = "paywalldata-empty-images.json"

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PaywallDataTest {
    @Test
    fun `test PaywallData properties`() {
        val paywall: PaywallData = decode(PAYWALLDATA_SAMPLE1)

        assertThat(paywall.templateName).isEqualTo("1")
        assertThat(paywall.assetBaseURL).isEqualTo(URL("https://rc-paywalls.s3.amazonaws.com"))
        assertThat(paywall.revision).isEqualTo(7)
        assertThat(paywall.config.packageIds).containsExactly("\$rc_monthly", "\$rc_annual", "custom_package")
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

        val unknownLocale = "gl_ES".toLocale()
        assertThat(paywall.configForLocale(unknownLocale)).isNull()

        val validLocale = "en_US".toLocale()
        val localizedConfiguration = paywall.configForLocale(validLocale)
        assertThat(localizedConfiguration).isNotNull
        assertThat(localizedConfiguration?.callToActionWithMultipleIntroOffers).isEqualTo(
            "Purchase now with multiple offers"
        )
        assertThat(localizedConfiguration?.offerDetailsWithMultipleIntroOffers).isEqualTo(
            "Try {{ sub_offer_duration }} for free, then {{ sub_offer_price_2 }} for your first " +
                "{{ sub_offer_duration_2 }}, and just {{ sub_price_per_month }} thereafter."
        )
    }

    @Test
    fun `finds locale if it only has language`() {
        val paywall: PaywallData = decode(PAYWALLDATA_SAMPLE1)

        val enConfig = paywall.configForLocale(Locale("en"))
        assertThat(enConfig?.title).isEqualTo("Paywall")

        val esConfig = paywall.configForLocale(Locale("es"))
        assertThat(esConfig?.title).isEqualTo("Tienda")
    }

    @Test
    fun `does not return a locale if no matching language`() {
        val paywall: PaywallData = decode(PAYWALLDATA_SAMPLE1)

        val enConfig = paywall.configForLocale(Locale("fr"))
        assertThat(enConfig).isNull()
    }

    @Test
    fun `if current locale is missing it loads available locale`() {
        val paywall: PaywallData = decode(PAYWALLDATA_MISSING_CURRENT_LOCALE)

        val localization = paywall.localizedConfiguration.second
        assertThat(localization.callToAction).isEqualTo("Comprar")
        assertThat(localization.title).isEqualTo("Tienda")
    }

    @Test
    fun `decodes empty images as null`() {
        val paywall: PaywallData = decode(PAYWALLDATA_EMPTY_IMAGES)

        val images = paywall.config.images
        assertThat(images.background).isNull()
        assertThat(images.header).isNull()
        assertThat(images.icon).isNull()
    }

    @Test
    fun `paywall color can be created from a ColorInt`() {
        val colorInt = Color.parseColor("#FFAABB")
        val paywallColor = PaywallColor(colorInt)

        assertThat(colorInt).isEqualTo(paywallColor.colorInt)
        assertThat("#FFAABB").isEqualTo(paywallColor.stringRepresentation)
        assertThat(Color.valueOf(colorInt)).isEqualTo(paywallColor.underlyingColor)
    }

    private fun loadJSON(jsonFileName: String) = File(javaClass.classLoader!!.getResource(jsonFileName).file).readText()
    private fun decode(file: String): PaywallData =
        OfferingParser.json.decodeFromString(loadJSON(file))
}
