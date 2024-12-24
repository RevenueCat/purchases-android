package com.revenuecat.purchases.ui.revenuecatui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallValidationResult
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatedPaywall
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LegacyPaywallDataValidationTest {

    @Test
    fun `Validate an offering without paywall`() {
        val offering = TestData.offeringWithNoPaywall
        val paywallValidationResult = getPaywallValidationResult(offering)
        assertThat(paywallValidationResult.errors?.size).isEqualTo(1)
        assertThat(paywallValidationResult.errors?.first()).isEqualTo(PaywallValidationError.MissingPaywall)
    }

    @Test
    fun `Validate a valid paywall`() {
        val offering = TestData.template1Offering
        val paywallValidationResult = getPaywallValidationResult(offering)
        assertThat(paywallValidationResult.errors).isNull()
    }

    @Test
    fun `Validate a valid multi-tier paywall`() {
        val offering = TestData.template7Offering
        val paywallValidationResult = getPaywallValidationResult(offering)
        assertThat(paywallValidationResult.errors).isNull()
    }

    @Test
    fun `Unrecognized template name generates default paywall`() {
        val templateName = "unrecognized_template"
        val originalOffering = TestData.template2Offering
        val offering = originalOffering.copy(paywall = originalOffering.paywall!!.copy(templateName = templateName))

        val paywallValidationResult = getPaywallValidationResult(offering)
        check(paywallValidationResult is PaywallValidationResult.Legacy)

        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        compareWithDefaultTemplate(paywallValidationResult.displayablePaywall)
        assertThat(paywallValidationResult.errors?.size).isEqualTo(1)
        assertThat(paywallValidationResult.errors?.first())
            .isEqualTo(PaywallValidationError.InvalidTemplate(templateName))
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
        check(paywallValidationResult is PaywallValidationResult.Legacy)
        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        compareWithDefaultTemplate(paywallValidationResult.displayablePaywall)
        assertThat(paywallValidationResult.errors?.size).isEqualTo(1)
        assertThat(paywallValidationResult.errors?.first()).isEqualTo(
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
        check(paywallValidationResult is PaywallValidationResult.Legacy)
        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        compareWithDefaultTemplate(paywallValidationResult.displayablePaywall)
        assertThat(paywallValidationResult.errors?.size).isEqualTo(1)
        assertThat(paywallValidationResult.errors?.first()).isEqualTo(
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
        check(paywallValidationResult is PaywallValidationResult.Legacy)
        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        compareWithDefaultTemplate(paywallValidationResult.displayablePaywall)
        assertThat(paywallValidationResult.errors?.size).isEqualTo(1)
        assertThat(paywallValidationResult.errors?.first()).isEqualTo(
            PaywallValidationError.InvalidIcons(setOf("an_unrecognized_icon"))
        )
    }

    @Test
    fun `No tiers in multi-tier config generate default paywall`() {
        val originalOffering = TestData.template7Offering

        val paywall = originalOffering.paywall!!.let { originalPaywall ->
            val originalConfig = originalPaywall.config

            val config = originalConfig.copy(tiers = emptyList())
            originalPaywall.copy(config = config)
        }

        val offering = originalOffering.copy(paywall = paywall)
        val paywallValidationResult = getPaywallValidationResult(offering)
        check(paywallValidationResult is PaywallValidationResult.Legacy)
        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        compareWithDefaultTemplate(
            paywallValidationResult.displayablePaywall,
            // Skipping because there are none but they will show in the paywall from createPackageConfiguration
            skipPackageIds = true,
        )
        assertThat(paywallValidationResult.errors?.size).isEqualTo(1)
        assertThat(paywallValidationResult.errors?.first()).isEqualTo(
            PaywallValidationError.MissingTiers
        )
    }

    @Test
    fun `Missing color tier in multi-tier config generate default paywall`() {
        val originalOffering = TestData.template7Offering

        val paywall = originalOffering.paywall!!.let { originalPaywall ->
            val originalConfig = originalPaywall.config

            val colorsByTier = originalConfig.colorsByTier!!.toMutableMap()
            colorsByTier.remove("basic")

            val config = originalConfig.copy(colorsByTier = colorsByTier)
            originalPaywall.copy(config = config)
        }

        val offering = originalOffering.copy(paywall = paywall)
        val paywallValidationResult = getPaywallValidationResult(offering)
        check(paywallValidationResult is PaywallValidationResult.Legacy)
        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        compareWithDefaultTemplate(
            paywallValidationResult.displayablePaywall,
            // Skipping because there are none but they will show in the paywall from createPackageConfiguration
            skipPackageIds = true,
        )
        assertThat(paywallValidationResult.errors?.size).isEqualTo(1)
        assertThat(paywallValidationResult.errors?.first()).isEqualTo(
            PaywallValidationError.MissingTierConfigurations(setOf("basic"))
        )
    }

    @Test
    fun `Missing image tier in multi-tier config only logs a warning`() {
        val originalOffering = TestData.template7Offering

        val paywall = originalOffering.paywall!!.let { originalPaywall ->
            val originalConfig = originalPaywall.config

            val imagesByTier = originalConfig.imagesByTier!!.toMutableMap()
            imagesByTier.remove("basic")

            val config = originalConfig.copy(imagesByTier = imagesByTier)
            originalPaywall.copy(config = config)
        }

        mockkObject(Logger)

        val offering = originalOffering.copy(paywall = paywall)
        val paywallValidationResult = getPaywallValidationResult(offering)
        check(paywallValidationResult is PaywallValidationResult.Legacy)
        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)

        assertThat(paywallValidationResult.errors).isNull()
        verify { Logger.w("Missing images for tier(s): basic") }
    }

    @Test
    fun `Missing localization tier in multi-tier config generate default paywall`() {
        val originalOffering = TestData.template7Offering

        val paywall = originalOffering.paywall!!.let { originalPaywall ->
            val (locale, originalTierLocalizationConfiguration) = originalPaywall.tieredLocalizedConfiguration

            val tierLocalizationConfiguration = originalTierLocalizationConfiguration.toMutableMap()
            tierLocalizationConfiguration.remove("basic")

            originalPaywall.copy(localizationByTier = mapOf(locale.toString() to tierLocalizationConfiguration))
        }

        val offering = originalOffering.copy(paywall = paywall)
        val paywallValidationResult = getPaywallValidationResult(offering)
        check(paywallValidationResult is PaywallValidationResult.Legacy)
        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        compareWithDefaultTemplate(
            paywallValidationResult.displayablePaywall,
            // Skipping because there are none but they will show in the paywall from createPackageConfiguration
            skipPackageIds = true,
        )
        assertThat(paywallValidationResult.errors?.size).isEqualTo(1)
        assertThat(paywallValidationResult.errors?.first()).isEqualTo(
            PaywallValidationError.MissingTierConfigurations(setOf("basic"))
        )
    }

    private fun getPaywallValidationResult(offering: Offering) = offering.validatedPaywall(
        currentColorScheme = TestData.Constants.currentColorScheme,
        resourceProvider = MockResourceProvider()
    )

    private fun verifyPackages(actual: PaywallData, expectation: PaywallData) {
        assertThat(actual.config.packageIds).isEqualTo(expectation.config.packageIds)
    }

    private fun compareWithDefaultTemplate(
        displayablePaywall: PaywallData,
        skipPackageIds: Boolean = false
    ) {
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

            if (!skipPackageIds) {
                assertThat(config.packageIds).containsExactly(*defaultConfig.packageIds.toTypedArray())
            }

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
