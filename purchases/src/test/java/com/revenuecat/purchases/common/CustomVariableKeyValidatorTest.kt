package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CustomVariableKeyValidatorTest {

    @Test
    fun `keys of letters, digits and underscores are valid`() {
        // Validation walks Unicode code points, so non-ASCII letters and digits, supplementary-plane letters
        // and Other_Alphabetic marks are all addressable as `custom.<key>`.
        val validKeys = listOf(
            "validKey",
            "valid_key_name",
            "key123",
            "player_score_2024",
            "a",
            "123key",
            "1alid",
            "_key",
            "kéy",
            "π_3",
            "\u0661valid",
            "\uD835\uDC9C",
            "\u0915\u093E",
            "a".repeat(1024),
        )

        assertThat(validKeys).allMatch { key -> CustomVariableKeyValidator.isValidKey(key) }
    }

    @Test
    fun `keys that cannot be addressed are invalid`() {
        val invalidKeys = listOf("", "key-name", "key name", "key.name", "key!", "key\uD83D\uDE42", "e\u0301")

        assertThat(invalidKeys).noneMatch { key -> CustomVariableKeyValidator.isValidKey(key) }
    }

    @Test
    fun `validateAndFilter keeps valid entries and drops invalid ones`() {
        val filtered = CustomVariableKeyValidator.validateAndFilter(
            mapOf(
                "valid_key" to "kept",
                "also.invalid" to "dropped",
                "also-invalid" to "dropped",
                "" to "dropped",
            ),
        )

        assertThat(filtered).isEqualTo(mapOf("valid_key" to "kept"))
    }

    @Test
    fun `validateAndFilter keeps everything when all keys are valid`() {
        val variables = mapOf("first" to 1, "second_key" to 2)

        assertThat(CustomVariableKeyValidator.validateAndFilter(variables)).isEqualTo(variables)
    }
}
