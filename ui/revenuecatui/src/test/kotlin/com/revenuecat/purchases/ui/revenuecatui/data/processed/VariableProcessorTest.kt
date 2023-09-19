package com.revenuecat.purchases.ui.revenuecatui.data.processed

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Package
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class VariableProcessorTest {

    private lateinit var variableDataProvider: VariableDataProvider
    private lateinit var rcPackage: Package
    private lateinit var locale: Locale

    @Before
    fun setUp() {
        variableDataProvider = mockk()
        rcPackage = mockk()
        locale = Locale.ITALY
    }

    @Test
    fun `process variables returns original text if no variables`() {
        val originalText = "text without any variables"
        val resultText = VariableProcessor.processVariables(variableDataProvider, originalText, rcPackage, locale)
        assertThat(resultText).isEqualTo(originalText)
    }

    @Test
    fun `process variables returns processed text with single variable`() {
        val originalText = "text with {{ app_name }} one variable"
        every { variableDataProvider.applicationName } returns "app name"
        val resultText = VariableProcessor.processVariables(variableDataProvider, originalText, rcPackage, locale)
        assertThat(resultText).isEqualTo("text with app name one variable")
    }

    @Test
    fun `process variables returns processed text with multiple variable`() {
        val originalText = "text with {{ app_name }} and {{ sub_price_per_month }} multiple variables"
        every { variableDataProvider.applicationName } returns "app name"
        every { variableDataProvider.localizedPricePerMonth(rcPackage, locale) } returns "$9.99"
        val resultText = VariableProcessor.processVariables(variableDataProvider, originalText, rcPackage, locale)
        assertThat(resultText).isEqualTo("text with app name and $9.99 multiple variables")
    }

    @Test
    fun `process variables does not modify unknown variables`() {
        val originalText = "text with {{ unknown_variable }}"
        val resultText = VariableProcessor.processVariables(variableDataProvider, originalText, rcPackage, locale)
        assertThat(resultText).isEqualTo(originalText)
    }
}
