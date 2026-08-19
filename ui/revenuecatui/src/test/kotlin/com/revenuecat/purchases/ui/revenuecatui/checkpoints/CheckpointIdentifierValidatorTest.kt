package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CheckpointIdentifierValidatorTest {

    @Test
    fun `valid checkpoint identifiers pass validation`() {
        val validIdentifiers = listOf(
            "a",
            "Z",
            "checkout",
            "checkout_123",
            "checkout-complete",
            "A-1_b",
            "a" + "1".repeat(CheckpointIdentifierValidator.MAX_LENGTH - 1),
        )

        validIdentifiers.forEach { identifier ->
            assertThat(CheckpointIdentifierValidator.isValid(identifier))
                .describedAs("Expected '%s' to be valid", identifier)
                .isTrue()
        }
    }

    @Test
    fun `invalid checkpoint identifiers fail validation`() {
        val invalidIdentifiers = listOf(
            "",
            "1checkout",
            "_checkout",
            "-checkout",
            "check out",
            " checkout",
            "checkout ",
            "checkout\n",
            "check.out",
            "chéckout",
            "checkout😀",
            "a" + "1".repeat(CheckpointIdentifierValidator.MAX_LENGTH),
        )

        invalidIdentifiers.forEach { identifier ->
            assertThat(CheckpointIdentifierValidator.isValid(identifier))
                .describedAs("Expected '%s' to be invalid", identifier)
                .isFalse()
        }
    }
}
