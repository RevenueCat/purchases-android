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
        )

        validIdentifiers.forEach { identifier ->
            assertThat(CheckpointIdentifierValidator.isValid(identifier))
                .describedAs("Expected '%s' to be valid", identifier)
                .isTrue()
        }
    }

    @Test
    fun `checkpoint identifier with 255 characters passes validation`() {
        val identifier = "a" + "1".repeat(254)

        assertThat(CheckpointIdentifierValidator.isValid(identifier)).isTrue()
    }

    @Test
    fun `checkpoint identifier longer than 255 characters fails validation`() {
        val identifier = "a" + "1".repeat(255)

        assertThat(CheckpointIdentifierValidator.isValid(identifier)).isFalse()
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
        )

        invalidIdentifiers.forEach { identifier ->
            assertThat(CheckpointIdentifierValidator.isValid(identifier))
                .describedAs("Expected '%s' to be invalid", identifier)
                .isFalse()
        }
    }
}
