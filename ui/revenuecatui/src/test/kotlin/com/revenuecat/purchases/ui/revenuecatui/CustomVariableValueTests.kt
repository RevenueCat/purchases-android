package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test

class CustomVariableValueTests {

    @Before
    fun setUp() {
        mockkObject(Logger)
        every { Logger.w(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
    }

    // region String conversion

    @Test
    fun `stringValue returns string value for String type`() {
        val value = CustomVariableValue.String("Hello")
        assertThat(value.stringValue).isEqualTo("Hello")
    }

    @Test
    fun `stringValue returns formatted integer for Number type with whole number`() {
        val value = CustomVariableValue.Number(100.0)
        assertThat(value.stringValue).isEqualTo("100")
    }

    @Test
    fun `stringValue returns decimal for Number type with fractional value`() {
        val value = CustomVariableValue.Number(99.99)
        assertThat(value.stringValue).isEqualTo("99.99")
    }

    @Test
    fun `stringValue returns true for Boolean true`() {
        val value = CustomVariableValue.Boolean(true)
        assertThat(value.stringValue).isEqualTo("true")
    }

    @Test
    fun `stringValue returns false for Boolean false`() {
        val value = CustomVariableValue.Boolean(false)
        assertThat(value.stringValue).isEqualTo("false")
    }

    // endregion

    // region Number constructors

    @Test
    fun `Number can be created from Int`() {
        val value = CustomVariableValue.Number(42)
        assertThat(value.value).isEqualTo(42.0)
        assertThat(value.stringValue).isEqualTo("42")
    }

    @Test
    fun `Number can be created from Long`() {
        val value = CustomVariableValue.Number(1234567890123L)
        assertThat(value.value).isEqualTo(1234567890123.0)
    }

    @Test
    fun `Number can be created from Float`() {
        val value = CustomVariableValue.Number(3.14f)
        assertThat(value.value).isCloseTo(3.14, org.assertj.core.data.Offset.offset(0.001))
    }

    // endregion

    // region from() factory method

    @Test
    fun `from creates String for kotlin String`() {
        val value = CustomVariableValue.from("test")
        assertThat(value).isEqualTo(CustomVariableValue.String("test"))
    }

    @Test
    fun `from creates Number for kotlin Int`() {
        val value = CustomVariableValue.from(42)
        assertThat(value).isEqualTo(CustomVariableValue.Number(42.0))
    }

    @Test
    fun `from creates Number for kotlin Long`() {
        val value = CustomVariableValue.from(123L)
        assertThat(value).isEqualTo(CustomVariableValue.Number(123.0))
    }

    @Test
    fun `from creates Number for kotlin Double`() {
        val value = CustomVariableValue.from(9.99)
        assertThat(value).isEqualTo(CustomVariableValue.Number(9.99))
    }

    @Test
    fun `from creates Number for kotlin Float`() {
        val value = CustomVariableValue.from(3.14f)
        assertThat(value).isInstanceOf(CustomVariableValue.Number::class.java)
    }

    @Test
    fun `from creates Boolean for kotlin Boolean true`() {
        val value = CustomVariableValue.from(true)
        assertThat(value).isEqualTo(CustomVariableValue.Boolean(true))
    }

    @Test
    fun `from creates Boolean for kotlin Boolean false`() {
        val value = CustomVariableValue.from(false)
        assertThat(value).isEqualTo(CustomVariableValue.Boolean(false))
    }

    // endregion

    // region from() with unsupported types

    @Test
    fun `from throws IllegalArgumentException for custom object`() {
        data class CustomObject(val name: String)
        val customObject = CustomObject("test")

        assertThatThrownBy { CustomVariableValue.from(customObject) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unsupported custom variable type")
            .hasMessageContaining("CustomObject")
    }

    @Test
    fun `from throws IllegalArgumentException for List`() {
        val list = listOf("a", "b", "c")

        assertThatThrownBy { CustomVariableValue.from(list) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unsupported custom variable type")
    }

    @Test
    fun `from throws IllegalArgumentException for Map`() {
        val map = mapOf("key" to "value")

        assertThatThrownBy { CustomVariableValue.from(map) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unsupported custom variable type")
    }

    @Test
    fun `from throws IllegalArgumentException for Array`() {
        val array = arrayOf(1, 2, 3)

        assertThatThrownBy { CustomVariableValue.from(array) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unsupported custom variable type")
    }

    // endregion

    // region Key validation

    @Test
    fun `valid key starting with letter is accepted and returned`() {
        val variables = mapOf("validKey" to CustomVariableValue.String("value"))
        val result = CustomVariableKeyValidator.validateAndFilter(variables)
        assertThat(result).isEqualTo(variables)
        verify(exactly = 0) { Logger.w(any()) }
    }

    @Test
    fun `valid key with underscores is accepted and returned`() {
        val variables = mapOf("valid_key_name" to CustomVariableValue.String("value"))
        val result = CustomVariableKeyValidator.validateAndFilter(variables)
        assertThat(result).isEqualTo(variables)
        verify(exactly = 0) { Logger.w(any()) }
    }

    @Test
    fun `valid key with numbers is accepted and returned`() {
        val variables = mapOf("key123" to CustomVariableValue.String("value"))
        val result = CustomVariableKeyValidator.validateAndFilter(variables)
        assertThat(result).isEqualTo(variables)
        verify(exactly = 0) { Logger.w(any()) }
    }

    @Test
    fun `valid key with mixed characters is accepted and returned`() {
        val variables = mapOf("player_score_2024" to CustomVariableValue.String("value"))
        val result = CustomVariableKeyValidator.validateAndFilter(variables)
        assertThat(result).isEqualTo(variables)
        verify(exactly = 0) { Logger.w(any()) }
    }

    @Test
    fun `invalid key starting with number logs warning and is filtered out`() {
        val variables = mapOf("123key" to CustomVariableValue.String("value"))
        val result = CustomVariableKeyValidator.validateAndFilter(variables)
        assertThat(result).isEmpty()
        verify { Logger.w(match { it.contains("123key") && it.contains("invalid") }) }
    }

    @Test
    fun `invalid key with special characters logs warning and is filtered out`() {
        val variables = mapOf("key-name" to CustomVariableValue.String("value"))
        val result = CustomVariableKeyValidator.validateAndFilter(variables)
        assertThat(result).isEmpty()
        verify { Logger.w(match { it.contains("key-name") && it.contains("invalid") }) }
    }

    @Test
    fun `invalid empty key logs warning and is filtered out`() {
        val variables = mapOf("" to CustomVariableValue.String("value"))
        val result = CustomVariableKeyValidator.validateAndFilter(variables)
        assertThat(result).isEmpty()
        verify { Logger.w(match { it.contains("invalid") }) }
    }

    @Test
    fun `invalid key with spaces logs warning and is filtered out`() {
        val variables = mapOf("key name" to CustomVariableValue.String("value"))
        val result = CustomVariableKeyValidator.validateAndFilter(variables)
        assertThat(result).isEmpty()
        verify { Logger.w(match { it.contains("key name") && it.contains("invalid") }) }
    }

    @Test
    fun `multiple invalid keys log warnings and are filtered out while valid keys are kept`() {
        val validValue = CustomVariableValue.String("value")
        val variables = mapOf(
            "valid_key" to validValue,
            "123invalid" to CustomVariableValue.String("value"),
            "also-invalid" to CustomVariableValue.String("value"),
        )
        val result = CustomVariableKeyValidator.validateAndFilter(variables)
        assertThat(result).isEqualTo(mapOf("valid_key" to validValue))
        verify(exactly = 2) { Logger.w(any()) }
    }

    // endregion
}
