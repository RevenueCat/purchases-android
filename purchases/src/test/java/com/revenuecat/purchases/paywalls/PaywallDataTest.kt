package com.revenuecat.purchases.paywalls

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import java.net.URL
import java.util.*

private const val PAYWALLDATA_SAMPLE1 = "paywalldata-sample1.json"

class PaywallDataTest {

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

        assertThat(paywall.config.colors.light.background.stringRepresentation).isEqualTo("#FF00AA")
        assertThat(paywall.config.colors.light.text1.stringRepresentation).isEqualTo("#FF00AA22")
        assertThat(paywall.config.colors.light.text2?.stringRepresentation).isEqualTo("#FF00AA11")
        assertThat(paywall.config.colors.light.callToActionBackground.stringRepresentation).isEqualTo("#FF00AACC")
        assertThat(paywall.config.colors.light.callToActionForeground.stringRepresentation).isEqualTo("#FF00AA")
        assertThat(paywall.config.colors.light.callToActionSecondaryBackground?.stringRepresentation).isEqualTo("#FF00BB")
        assertThat(paywall.config.colors.light.accent1?.stringRepresentation).isEqualTo("#FF0000")
        assertThat(paywall.config.colors.light.accent2?.stringRepresentation).isEqualTo("#00FF00")

        assertThat(paywall.config.colors.dark?.background?.stringRepresentation).isEqualTo("#FF0000")
        assertThat(paywall.config.colors.dark?.text1?.stringRepresentation).isEqualTo("#FF0011")
        assertThat(paywall.config.colors.dark?.text2).isNull()
        assertThat(paywall.config.colors.dark?.callToActionBackground?.stringRepresentation).isEqualTo("#112233AA")
        assertThat(paywall.config.colors.dark?.callToActionForeground?.stringRepresentation).isEqualTo("#AABBCC")
        assertThat(paywall.config.colors.dark?.accent1?.stringRepresentation).isEqualTo("#00FFFF")
        assertThat(paywall.config.colors.dark?.accent2?.stringRepresentation).isEqualTo("#FF00FF")

        assertThat(paywall.configForLocale(Locale("gl_ES"))).isNull()
    }
    
}
