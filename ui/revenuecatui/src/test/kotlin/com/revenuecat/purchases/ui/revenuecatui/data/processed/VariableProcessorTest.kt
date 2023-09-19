package com.revenuecat.purchases.ui.revenuecatui.data.processed

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.data.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.ApplicationContext
import io.mockk.every
import io.mockk.mockk
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
        applicationContext = object : ApplicationContext {
            override fun getApplicationName(): String {
                return "test app name"
            }
        }
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
        val expectedText = "text with test app name one variable"
        expectVariablesResult(originalText, expectedText)
    }

    @Test
    fun `process variables returns processed text with multiple variable`() {
        val originalText = "text with {{ app_name }} and {{ sub_price_per_month }} multiple variables"
        val expectedText = "text with test app name and $5.67 multiple variables"
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
        expectVariablesResult("{{ app_name }}", "test app name")
    }

    @Test
    fun `process variables processes price`() {
        expectVariablesResult("{{ price }}", "$67.99")
    }

    @Test
    fun `process variables processes price_per_period`() {
        expectVariablesResult("{{ price_per_period }}", "PRICE_PER_PERIOD")
    }

    @Test
    fun `process variables processes total_price_and_per_month`() {
        expectVariablesResult("{{ total_price_and_per_month }}", "PRICE_AND_PER_MONTH")
    }

    @Test
    fun `process variables processes product_name`() {
        expectVariablesResult("{{ product_name }}", "PRODUCT_NAME")
    }

    @Test
    fun `process variables processes sub_period`() {
        expectVariablesResult("{{ sub_period }}", "PERIOD_NAME")
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
        expectVariablesResult("{{ sub_duration }}", "SUBS_DURATION")
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

    private fun expectVariablesResult(originalText: String, expectedText: String, locale: Locale = usLocale) {
        val resultText = VariableProcessor.processVariables(variableDataProvider, originalText, rcPackage, locale)
        assertThat(resultText).isEqualTo(expectedText)
    }
}
