package com.revenuecat.purchases.ui.revenuecatui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.offerings.offeringWithMultiPackagePaywall
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatedPaywall
import io.mockk.mockk
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PaywallDataValidationTest {

    @Before
    fun setUp() {
    }

    @Test
    fun `Validate an offering without paywall`() {
        val offering = TestData.offeringWithNoPaywall
        val paywallValidationResult = offering.validatedPaywall(
            currentColorScheme = TestData.Constants.currentColorScheme,
        )
        assertThat(paywallValidationResult.error).isEqualTo(PaywallValidationError.MissingPaywall)
    }

    @Test
    fun `Validate a valid paywall`() {
        val offering = TestData.template1Offering
        val paywallValidationResult = offering.validatedPaywall(
            currentColorScheme = TestData.Constants.currentColorScheme,
        )
        assertThat(paywallValidationResult.error).isNull()
    }

    @Test
    fun `Unrecognized template name generates default paywall`() {
        val templateName = "unrecognized_template"
        val originalOffering = TestData.offeringWithMultiPackagePaywall
        val offering = originalOffering.copy(paywall = originalOffering.paywall!!.copy(templateName = templateName))

        val paywallValidationResult = offering.validatedPaywall(
            currentColorScheme = TestData.Constants.currentColorScheme,
        )

        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        compareWithJson(
            paywallValidationResult.displayablePaywall,
            "default_template.json"
        )
        assertThat(paywallValidationResult.error).isEqualTo(PaywallValidationError.InvalidTemplate(templateName))
    }

    @Test
    fun `Unrecognized variable generates default paywall`() {
        val originalOffering = TestData.offeringWithMultiPackagePaywall

        val paywall = originalOffering.paywall!!.let { originalPaywall ->
            val (locale, originalLocalizedConfiguration) = originalPaywall.localizedConfiguration
            val localizedConfiguration = originalLocalizedConfiguration.copy(
                title = "Title with {{ unrecognized_variable }}",
                callToAction = "{{ future_variable }}",
            )
            originalPaywall.copy(localization = mapOf(locale.toString() to localizedConfiguration))
        }

        val offering = originalOffering.copy(paywall = paywall)
        val paywallValidationResult = offering.validatedPaywall(
            currentColorScheme = TestData.Constants.currentColorScheme,
        )
        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        compareWithJson(
            paywallValidationResult.displayablePaywall,
            "default_template.json"
        )
        assertThat(paywallValidationResult.error).isEqualTo(
            PaywallValidationError.InvalidVariables(setOf("unrecognized_variable", "future_variable"))
        )
    }

    private fun verifyPackages(actual: PaywallData, expectation: PaywallData) {
        assertThat(actual.config.packages).isEqualTo(expectation.config.packages)
    }

    private fun compareWithJson(displayablePaywall: PaywallData, jsonFileName: String) {
        val json = loadJSON(jsonFileName)
        val paywall: PaywallData = Json.decodeFromString(json)
        assertThat(displayablePaywall).isEqualTo(paywall)
    }

    private fun loadJSON(jsonFileName: String) = File(javaClass.classLoader!!.getResource(jsonFileName).file).readText()
}