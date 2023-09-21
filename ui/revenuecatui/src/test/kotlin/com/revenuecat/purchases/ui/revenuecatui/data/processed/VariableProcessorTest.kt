package com.revenuecat.purchases.ui.revenuecatui.data.processed

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ui.revenuecatui.data.MockApplicationContext
import com.revenuecat.purchases.ui.revenuecatui.data.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.ApplicationContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class VariableProcessorTest {

    private val usLocale = Locale.US
    private val esLocale = Locale("es", "ES")

    private lateinit var applicationContext: ApplicationContext
    private lateinit var variableDataProvider: VariableDataProvider
    private lateinit var rcPackage: Package

    @Before
    fun setUp() {
        applicationContext = MockApplicationContext()
        variableDataProvider = VariableDataProvider(applicationContext)
        rcPackage = TestData.Packages.annual
    }

    @Test
    fun `process variables returns original text if empty string`() {
        val originalText = ""
        expectVariablesResult(originalText, originalText)
    }

    @Test
    fun `process variables returns original text if no variables`() {
        val originalText = "text without any variables"
        expectVariablesResult(originalText, originalText)
    }

    @Test
    fun `process variables returns processed text with single variable`() {
        val originalText = "text with {{ app_name }} one variable"
        val expectedText = "text with Mock Paywall one variable"
        expectVariablesResult(originalText, expectedText)
    }

    @Test
    fun `process variables returns processed text with multiple variable`() {
        val originalText = "text with {{ app_name }} and {{ sub_price_per_month }} multiple variables"
        val expectedText = "text with Mock Paywall and $5.67 multiple variables"
        expectVariablesResult(originalText, expectedText)
    }

    @Test
    fun `process variables does not modify unknown variables`() {
        val originalText = "text with {{ unknown_variable }}"
        expectVariablesResult(originalText, originalText)
    }

    @Test
    fun `process variables does not process variable if no spaces`() {
        val originalText = "text with {{app_name}} and something"
        expectVariablesResult(originalText, originalText)
    }

    // region Variables

    @Test
    fun `process variables processes app_name`() {
        expectVariablesResult("{{ app_name }}", "Mock Paywall")
    }

    @Test
    fun `process variables processes price`() {
        expectVariablesResult("{{ price }}", "$67.99")
    }

    @Test
    fun `process variables processes price_per_period`() {
        expectVariablesResult("{{ price_per_period }}", "$67.99/yr")
        expectVariablesResult("{{ price_per_period }}", "$7.99/mth", rcPackage = TestData.Packages.monthly)
        expectVariablesResult("{{ price_per_period }}", "$1.99/wk", rcPackage = TestData.Packages.weekly)
        expectVariablesResult("{{ price_per_period }}", "$15.99/2 mths", rcPackage = TestData.Packages.bimonthly)
        expectVariablesResult("{{ price_per_period }}", "$23.99/3 mths", rcPackage = TestData.Packages.quarterly)
        expectVariablesResult("{{ price_per_period }}", "$39.99/6 mths", rcPackage = TestData.Packages.semester)
        expectVariablesResult("{{ price_per_period }}", "$1,000", rcPackage = TestData.Packages.lifetime)
    }

    @Test
    fun `process variables processes price_per_period localized in spanish`() {
        expectVariablesResult("{{ price_per_period }}", "$67.99/a", esLocale)
        expectVariablesResult("{{ price_per_period }}", "$7.99/m.", esLocale, TestData.Packages.monthly)
        expectVariablesResult("{{ price_per_period }}", "$1.99/sem.", esLocale, TestData.Packages.weekly)
        expectVariablesResult("{{ price_per_period }}", "$15.99/2 m.", esLocale, TestData.Packages.bimonthly)
        expectVariablesResult("{{ price_per_period }}", "$23.99/3 m.", esLocale, TestData.Packages.quarterly)
        expectVariablesResult("{{ price_per_period }}", "$39.99/6 m.", esLocale, TestData.Packages.semester)
        expectVariablesResult("{{ price_per_period }}", "$1,000", esLocale, TestData.Packages.lifetime)
    }

    @Test
    fun `process variables processes total_price_and_per_month`() {
        expectVariablesResult("{{ total_price_and_per_month }}", "PRICE_AND_PER_MONTH")
    }

    @Test
    fun `process variables processes product_name`() {
        expectVariablesResult("{{ product_name }}", "Annual", rcPackage = TestData.Packages.annual)
        expectVariablesResult("{{ product_name }}", "Monthly", rcPackage = TestData.Packages.monthly)
        expectVariablesResult("{{ product_name }}", "Weekly", rcPackage = TestData.Packages.weekly)
        expectVariablesResult("{{ product_name }}", "2 month", rcPackage = TestData.Packages.bimonthly)
        expectVariablesResult("{{ product_name }}", "3 month", rcPackage = TestData.Packages.quarterly)
        expectVariablesResult("{{ product_name }}", "6 month", rcPackage = TestData.Packages.semester)
        expectVariablesResult("{{ product_name }}", "Lifetime", rcPackage = TestData.Packages.lifetime)
    }

    @Test
    fun `process variables processes sub_period`() {
        expectVariablesResult("{{ sub_period }}", "Annual", rcPackage = TestData.Packages.annual)
        expectVariablesResult("{{ sub_period }}", "Monthly", rcPackage = TestData.Packages.monthly)
        expectVariablesResult("{{ sub_period }}", "Weekly", rcPackage = TestData.Packages.weekly)
        expectVariablesResult("{{ sub_period }}", "2 month", rcPackage = TestData.Packages.bimonthly)
        expectVariablesResult("{{ sub_period }}", "3 month", rcPackage = TestData.Packages.quarterly)
        expectVariablesResult("{{ sub_period }}", "6 month", rcPackage = TestData.Packages.semester)
        expectVariablesResult("{{ sub_period }}", "Lifetime", rcPackage = TestData.Packages.lifetime)
    }

    @Test
    fun `process variables processes sub_period custom period`() {
        rcPackage = TestData.Packages.annual.copy(packageType = PackageType.CUSTOM)
        expectVariablesResult("{{ sub_period }}", "")
    }

    @Test
    fun `process variables processes sub_price_per_month`() {
        expectVariablesResult("{{ sub_price_per_month }}", "$5.67")
    }

    @Test
    fun `process variables processes sub_price_per_month in other locales`() {
        expectVariablesResult("{{ sub_price_per_month }}", "5,67 US$", esLocale)
    }

    @Test
    fun `process variables processes sub_duration`() {
        expectVariablesResult("{{ sub_duration }}", "1 year", rcPackage = TestData.Packages.annual)
        expectVariablesResult("{{ sub_duration }}", "1 month", rcPackage = TestData.Packages.monthly)
        expectVariablesResult("{{ sub_duration }}", "1 week", rcPackage = TestData.Packages.weekly)
        expectVariablesResult("{{ sub_duration }}", "2 months", rcPackage = TestData.Packages.bimonthly)
        expectVariablesResult("{{ sub_duration }}", "3 months", rcPackage = TestData.Packages.quarterly)
        expectVariablesResult("{{ sub_duration }}", "6 months", rcPackage = TestData.Packages.semester)
    }

    @Test
    fun `process variables processes sub_duration falls back to period name if no period`() {
        expectVariablesResult("{{ sub_duration }}", "Lifetime", rcPackage = TestData.Packages.lifetime)
    }

    @Test
    fun `process variables processes sub_duration in spanish`() {
        expectVariablesResult("{{ sub_duration }}", "1 año", esLocale, TestData.Packages.annual)
        expectVariablesResult("{{ sub_duration }}", "1 mes", esLocale, TestData.Packages.monthly)
        expectVariablesResult("{{ sub_duration }}", "1 semana", esLocale, TestData.Packages.weekly)
        expectVariablesResult("{{ sub_duration }}", "2 meses", esLocale, TestData.Packages.bimonthly)
        expectVariablesResult("{{ sub_duration }}", "3 meses", esLocale, TestData.Packages.quarterly)
        expectVariablesResult("{{ sub_duration }}", "6 meses", esLocale, TestData.Packages.semester)
        // Not using real context so can't access localized version
        expectVariablesResult("{{ sub_duration }}", "Lifetime", esLocale, TestData.Packages.lifetime)
    }

    @Test
    fun `process variables processes sub_offer_duration`() {
        expectVariablesResult("{{ sub_offer_duration }}", "INT_OFFER_DURATION")
    }

    @Test
    fun `process variables processes sub_offer_price`() {
        expectVariablesResult("{{ sub_offer_price }}", "INTRO_OFFER_PRICE")
    }

    // endregion Variables

    private fun expectVariablesResult(
        originalText: String,
        expectedText: String,
        locale: Locale = usLocale,
        rcPackage: Package = this.rcPackage,
    ) {
        val resultText = VariableProcessor.processVariables(variableDataProvider, originalText, rcPackage, locale)
        assertThat(resultText).isEqualTo(expectedText)
    }
}
