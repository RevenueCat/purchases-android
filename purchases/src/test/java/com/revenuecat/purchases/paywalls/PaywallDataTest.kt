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
private const val PAYWALLDATA_CHINESE = "paywalldata-chinese.json"
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
        assertThat(paywall.config.images.header).isEqualTo("header.webp")
        assertThat(paywall.config.images.background).isEqualTo("background.jpg")
        assertThat(paywall.config.images.icon).isEqualTo("icon.webp")
        assertThat(paywall.config.blurredBackgroundImage).isTrue
        assertThat(paywall.config.displayRestorePurchases).isFalse
        assertThat(paywall.config.termsOfServiceURL).isEqualTo(URL("https://revenuecat.com/tos"))
        assertThat(paywall.config.privacyURL).isEqualTo(URL("https://revenuecat.com/privacy"))

        paywall.config.colors.light.let { lightColors ->
            assertThat(lightColors.background.stringRepresentation).isEqualTo("#FF00AA")
            assertThat(lightColors.text1.stringRepresentation).isEqualTo("#FF00AA22")
            assertThat(lightColors.text2?.stringRepresentation).isEqualTo("#FF00AA11")
            assertThat(lightColors.text3?.stringRepresentation).isEqualTo("#FF00AA33")
            assertThat(lightColors.callToActionBackground.stringRepresentation).isEqualTo("#FF00AACC")
            assertThat(lightColors.callToActionForeground.stringRepresentation).isEqualTo("#FF00AA")
            assertThat(lightColors.callToActionSecondaryBackground?.stringRepresentation).isEqualTo("#FF00BB")
            assertThat(lightColors.accent1?.stringRepresentation).isEqualTo("#FF0000")
            assertThat(lightColors.accent2?.stringRepresentation).isEqualTo("#00FF00")
            assertThat(lightColors.accent3?.stringRepresentation).isEqualTo("#0000FF")
        }

        assertThat(paywall.config.colors.dark).isNotNull
        paywall.config.colors.dark?.let { darkColors ->
            assertThat(darkColors.background.stringRepresentation).isEqualTo("#FF0000")
            assertThat(darkColors.text1.stringRepresentation).isEqualTo("#FF0011")
            assertThat(darkColors.text2).isNull()
            assertThat(darkColors.text3).isNull()
            assertThat(darkColors.callToActionBackground.stringRepresentation).isEqualTo("#112233AA")
            assertThat(darkColors.callToActionForeground.stringRepresentation).isEqualTo("#AABBCC")
            assertThat(darkColors.accent1?.stringRepresentation).isEqualTo("#00FFFF")
            assertThat(darkColors.accent2?.stringRepresentation).isEqualTo("#FF00FF")
            assertThat(darkColors.accent3?.stringRepresentation).isNull()
        }

        val unknownLocale = "gl_ES".toLocale()
        assertThat(paywall.configForLocale(unknownLocale)).isNull()

        val english = paywall.configForLocale("en_US".toLocale())
        assertThat(english).isNotNull
        english?.apply {
            assertThat(title).isEqualTo("Paywall")
            assertThat(subtitle).isEqualTo("Description")

            assertThat(callToAction).isEqualTo("Purchase now")
            assertThat(callToActionWithIntroOffer).isEqualTo("Purchase now")
            assertThat(callToActionWithMultipleIntroOffers).isEqualTo("Purchase now with multiple offers")

            assertThat(offerDetails).isEqualTo("{{ sub_price_per_month }} per month")
            assertThat(offerDetailsWithIntroOffer).isEqualTo(
                "Start your {{ sub_offer_duration }} trial, " +
                    "then {{ sub_price_per_month }} per month"
            )
            assertThat(offerDetailsWithMultipleIntroOffers).isEqualTo(
                "Try {{ sub_offer_duration }} for free, " +
                    "then {{ sub_offer_price_2 }} for your first {{ sub_offer_duration_2 }}, " +
                    "and just {{ sub_price_per_month }} thereafter."
            )
        }

        val spanish = paywall.configForLocale("es".toLocale())
        assertThat(spanish).isNotNull
        spanish?.apply {
            assertThat(title).isEqualTo("Tienda")
            assertThat(subtitle).isNull()

            assertThat(callToAction).isEqualTo("Comprar")
            assertThat(callToActionWithIntroOffer).isNull()
            assertThat(callToActionWithMultipleIntroOffers).isNull()

            assertThat(offerDetails).isNull()
            assertThat(offerDetailsWithIntroOffer).isNull()
            assertThat(offerDetailsWithMultipleIntroOffers).isNull()
        }
    }

    @Test
    fun `Chinese localization`() {
        val paywall: PaywallData = decode(PAYWALLDATA_CHINESE)

        val traditional = paywall.configForLocale("zh-Hant".toLocale())
        val simplified = paywall.configForLocale("zh-Hans".toLocale())
        val taiwan = paywall.configForLocale("zh-TW".toLocale())
        assertThat(traditional).isNotNull
        assertThat(simplified).isNotNull
        assertThat(taiwan).isNotNull

        assertThat(traditional?.title).isEqualTo("Traditional")
        assertThat(simplified?.title).isEqualTo("Simplified")
        assertThat(taiwan?.title).isEqualTo("Traditional")
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
    fun `localized configuration finds locale with different region`() {
        val paywall: PaywallData = decode(PAYWALLDATA_SAMPLE1)

        val configuration = paywall.findLocalizedConfiguration(
            locales = listOf(
                Locale("en", "IN")
            )
        )
        assertThat(configuration).isNotNull
        assertThat(configuration.second.title).isEqualTo("Paywall")
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
    fun `does not fail to decode invalid URLs`() {
        val paywall: PaywallData = decode(PAYWALLDATA_EMPTY_IMAGES)

        assertThat(paywall.config.privacyURL).isNull()
        assertThat(paywall.config.termsOfServiceURL).isNull()
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
