package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatedPaywall
import io.mockk.every
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
            currentColorScheme = mockk(relaxed = true),
        )
        assertThat(paywallValidationResult.error).isEqualTo(PaywallValidationError.MissingPaywall)
    }

    @Test
    fun `Validate a valid paywall`() {
        val offering = TestData.template1Offering
        val paywallValidationResult = offering.validatedPaywall(
            currentColorScheme = mockk(relaxed = true),
        )
        assertThat(paywallValidationResult.error).isNull()
    }

    @Test
    fun `Unrecognized template name generates default paywall`() {
        val templateName = "unrecognized_template"
        val originalOffering = TestData.offeringWithMultiPackagePaywall
        val offering = originalOffering.copy(paywall = originalOffering.paywall!!.copy(templateName = templateName))
        val currentColorScheme = ColorScheme(
            primary = Color.White,
            onPrimary = Color.White,
            primaryContainer = Color.White,
            onPrimaryContainer = Color.White,
            inversePrimary = Color.Green,
            secondary = Color.Black,
            onSecondary = Color.Black,
            secondaryContainer = Color.Black,
            onSecondaryContainer = Color.Black,
            tertiary = Color.Cyan,
            onTertiary = Color.Black,
            tertiaryContainer = Color.Gray,
            onTertiaryContainer = Color.White,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.Gray,
            onSurface = Color.Black,
            surfaceVariant = Color.DarkGray,
            onSurfaceVariant = Color.White,
            surfaceTint = Color.LightGray,
            inverseSurface = Color.Black,
            inverseOnSurface = Color.White,
            error = Color.Red,
            onError = Color.White,
            errorContainer = Color.Red,
            onErrorContainer = Color.White,
            outline = Color.Transparent,
            outlineVariant = Color.LightGray,
            scrim = Color.Gray
        )

        val paywallValidationResult = offering.validatedPaywall(
            currentColorScheme = currentColorScheme,
        )

        verifyPackages(paywallValidationResult.displayablePaywall, originalOffering.paywall!!)
        snapshot(paywallValidationResult.displayablePaywall)
        assertThat(paywallValidationResult.error).isEqualTo(PaywallValidationError.InvalidTemplate(templateName))
    }

    private fun verifyPackages(actual: PaywallData, expectation: PaywallData) {
        assertThat(actual.config.packages).isEqualTo(expectation.config.packages)
    }

    private fun snapshot(displayablePaywall: PaywallData) {
        val json = loadJSON("testUnrecognizedTemplateNameGeneratesDefaultPaywall.1.json")
        val paywall: PaywallData = Json.decodeFromString(json)
        assertThat(displayablePaywall).isEqualTo(paywall)
    }

    private fun loadJSON(jsonFileName: String) = File(javaClass.classLoader!!.getResource(jsonFileName).file).readText()

}