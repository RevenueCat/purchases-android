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
    fun `keys of letters, digits and underscores starting with a letter are valid`() {
        // isLetter is Unicode-aware, and a non-ASCII key is still addressable as `custom.<key>`.
        val validKeys = listOf("validKey", "valid_key_name", "key123", "player_score_2024", "a", "kéy")

        assertThat(validKeys).allMatch { key -> CustomVariableKeyValidator.isValidKey(key) }
    }

    @Test
    fun `keys that cannot be addressed are invalid`() {
        val invalidKeys = listOf("", "123key", "_key", "key-name", "key name", "key.name", "key!")

        assertThat(invalidKeys).noneMatch { key -> CustomVariableKeyValidator.isValidKey(key) }
    }

    @Test
    fun `validateAndFilter keeps valid entries and drops invalid ones`() {
        val filtered = CustomVariableKeyValidator.validateAndFilter(
            mapOf(
                "valid_key" to "kept",
                "123invalid" to "dropped",
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
