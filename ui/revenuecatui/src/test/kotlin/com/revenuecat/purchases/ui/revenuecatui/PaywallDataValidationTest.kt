package com.revenuecat.purchases.ui.revenuecatui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockApplicationContext
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatedPaywall
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PaywallDataValidationTest {

    @Test
    fun `Validate an offering without paywall`() {
        val offering = TestData.offeringWithNoPaywall
        val paywallValidationResult = getPaywallValidationResult(offering)
        assertThat(paywallValidationResult.error).isEqualTo(PaywallValidationError.MissingPaywall)
    }

    @Test
    fun `Validate a valid paywall`() {
        val offering = TestData.template1Offering
        val paywallValidationResult = getPaywallValidationResult(offering)
        assertThat(paywallValidationResult.error).isNull()
    }

    @Test
    fun `Unrecognized template name generates default paywall`() {
        val templateName = "unrecognized_template"
        val originalOffering = TestData.template2Offering
        val offering = originalOffering.copy(paywall = originalOffering.paywall!!.copy(templateName = templateName))

        val paywallValidationResult = getPaywallValidationResult(offering)

        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        compareWithDefaultTemplate(paywallValidationResult.displayablePaywall)
        assertThat(paywallValidationResult.error).isEqualTo(PaywallValidationError.InvalidTemplate(templateName))
    }

    @Test
    fun `Unrecognized variable generates default paywall`() {
        val originalOffering = TestData.template2Offering

        val paywall = originalOffering.paywall!!.let { originalPaywall ->
            val (locale, originalLocalizedConfiguration) = originalPaywall.localizedConfiguration
            val localizedConfiguration = originalLocalizedConfiguration.copy(
                title = "Title with {{ unrecognized_variable }}",
                callToAction = "{{ future_variable }}",
            )
            originalPaywall.copy(localization = mapOf(locale.toString() to localizedConfiguration))
        }

        val offering = originalOffering.copy(paywall = paywall)
        val paywallValidationResult = getPaywallValidationResult(offering)
        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        compareWithDefaultTemplate(paywallValidationResult.displayablePaywall)
        assertThat(paywallValidationResult.error).isEqualTo(
            PaywallValidationError.InvalidVariables(setOf("unrecognized_variable", "future_variable"))
        )
    }

    @Test
    fun `Unrecognized variables in features generate default paywall`() {
        val originalOffering = TestData.template2Offering

        val paywall = originalOffering.paywall!!.let { originalPaywall ->
            val (locale, originalLocalizedConfiguration) = originalPaywall.localizedConfiguration
            val localizedConfiguration = originalLocalizedConfiguration.copy(
                features = listOf(
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "{{ future_variable }}",
                        content = "{{ new_variable }}"
                    ),
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "{{ another_one }}"
                    )
                )
            )
            originalPaywall.copy(localization = mapOf(locale.toString() to localizedConfiguration))
        }

        val offering = originalOffering.copy(paywall = paywall)
        val paywallValidationResult = getPaywallValidationResult(offering)
        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        compareWithDefaultTemplate(paywallValidationResult.displayablePaywall)
        assertThat(paywallValidationResult.error).isEqualTo(
            PaywallValidationError.InvalidVariables(setOf("future_variable", "new_variable", "another_one"))
        )
    }

    @Test
    fun `Unrecognized icons generate default paywall`() {
        val originalOffering = TestData.template2Offering

        val paywall = originalOffering.paywall!!.let { originalPaywall ->
            val (locale, originalLocalizedConfiguration) = originalPaywall.localizedConfiguration
            val localizedConfiguration = originalLocalizedConfiguration.copy(
                features = listOf(
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Feature Title",
                        iconID = "an_unrecognized_icon",
                    ),
                )
            )
            originalPaywall.copy(localization = mapOf(locale.toString() to localizedConfiguration))
        }

        val offering = originalOffering.copy(paywall = paywall)
        val paywallValidationResult = getPaywallValidationResult(offering)
        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        compareWithDefaultTemplate(paywallValidationResult.displayablePaywall)
        assertThat(paywallValidationResult.error).isEqualTo(
            PaywallValidationError.InvalidIcons(setOf("an_unrecognized_icon"))
        )
    }

    private fun getPaywallValidationResult(offering: Offering) = offering.validatedPaywall(
        currentColorScheme = TestData.Constants.currentColorScheme,
        applicationContext = MockApplicationContext()
    )

    private fun verifyPackages(actual: PaywallData, expectation: PaywallData) {
        assertThat(actual.config.packageIds).isEqualTo(expectation.config.packageIds)
    }

    private fun compareWithDefaultTemplate(displayablePaywall: PaywallData) {
        val json = File(javaClass.classLoader!!.getResource("default_template.json").file).readText()
        val defaultPaywall: PaywallData = Json.decodeFromString(json)

        assertThat(displayablePaywall.assetBaseURL).isEqualTo(defaultPaywall.assetBaseURL)
        assertThat(displayablePaywall.templateName).isEqualTo(defaultPaywall.templateName)
        assertThat(displayablePaywall.revision).isEqualTo(defaultPaywall.revision)

        (displayablePaywall.config to defaultPaywall.config).let { (config, defaultConfig) ->
            assertThat(config.blurredBackgroundImage).isEqualTo(defaultConfig.blurredBackgroundImage)
            assertColors(config.colors.light, defaultConfig.colors.light)
            assertColors(config.colors.dark!!, defaultConfig.colors.dark!!)
            assertThat(config.displayRestorePurchases).isEqualTo(defaultConfig.displayRestorePurchases)
            assertThat(config.images.background).isEqualTo(defaultConfig.images.background)
            assertThat(config.images.header).isEqualTo(defaultConfig.images.header)
            assertThat(config.images.icon).isEqualTo(defaultConfig.images.icon)
            assertThat(config.packageIds).containsExactly(*defaultConfig.packageIds.toTypedArray())
            assertThat(config.defaultPackage).isEqualTo(defaultConfig.defaultPackage)
            assertThat(config.termsOfServiceURL).isEqualTo(defaultConfig.termsOfServiceURL)
            assertThat(config.privacyURL).isEqualTo(defaultConfig.privacyURL)
        }

        assertThat(displayablePaywall.localizedConfiguration).isEqualTo(defaultPaywall.localizedConfiguration)
    }

    private fun assertColors(
        actualColors: PaywallData.Configuration.Colors,
        defaultColors: PaywallData.Configuration.Colors,
    ) {
        assertThat(actualColors.accent1).isEqualTo(defaultColors.accent1)
        assertThat(actualColors.accent2).isEqualTo(defaultColors.accent2)
        assertThat(actualColors.accent3).isEqualTo(defaultColors.accent3)
        assertThat(actualColors.background).isEqualTo(defaultColors.background)
        assertThat(actualColors.callToActionBackground).isEqualTo(defaultColors.callToActionBackground)
        assertThat(actualColors.callToActionForeground).isEqualTo(defaultColors.callToActionForeground)
        assertThat(actualColors.text1).isEqualTo(defaultColors.text1)
        assertThat(actualColors.text2).isEqualTo(defaultColors.text2)
        assertThat(actualColors.text3).isEqualTo(defaultColors.text3)
    }
}
