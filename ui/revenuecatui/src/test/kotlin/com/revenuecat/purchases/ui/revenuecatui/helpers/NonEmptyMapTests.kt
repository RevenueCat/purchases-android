package com.revenuecat.purchases.ui.revenuecatui.helpers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NonEmptyMapTests {

    @Test
    fun `Value from entry should take precedence over value from map with the same key`() {
        // Arrange
        // `entry` has a different value than `map`, for the same key.
        val key = 0
        val expectedValue = true
        val entry = key to expectedValue
        val map = mapOf(
            key to !expectedValue,
            key + 1 to true,
            key + 2 to false,
        )

        // Act
        val nonEmptyMap = nonEmptyMapOf(entry, map)
        val actual = nonEmptyMap[key]

        // Assert
        assertThat(actual).isEqualTo(expectedValue)
    }
}
