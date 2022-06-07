package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Store
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class APIKeyValidatorTest {

    @Test
    fun `Validation result is google key but amazon store if API key is from google but store is amazon`() {
        val expected = APIKeyValidator.ValidationResult.GOOGLE_KEY_AMAZON_STORE
        val validator = APIKeyValidator()
        val validationResult = validator.validate("goog_1a2b3c4d5e6f7h", Store.AMAZON)
        assertThat(validationResult).isEqualTo(expected)
    }

    @Test
    fun `Validation result is amazon key but google store if API key is from amazon but store is google`() {
        val expected = APIKeyValidator.ValidationResult.AMAZON_KEY_GOOGLE_STORE
        val validator = APIKeyValidator()
        val validationResult = validator.validate("amazon_1a2b3c4d5e6f7h", Store.PLAY_STORE)
        assertThat(validationResult).isEqualTo(expected)
    }

    @Test
    fun `Validation result is legacy`() {
        val expected = APIKeyValidator.ValidationResult.LEGACY
        val validator = APIKeyValidator()
        val validationResult = validator.validate("1a2b3c4d5e6f7h", Store.PLAY_STORE)
        assertThat(validationResult).isEqualTo(expected)
    }

    @Test
    fun `Validation result is valid if google key and google store`() {
        val expected = APIKeyValidator.ValidationResult.VALID
        val validator = APIKeyValidator()
        val validationResult = validator.validate("goog_1a2b3c4d5e6f7h", Store.PLAY_STORE)
        assertThat(validationResult).isEqualTo(expected)
    }

    @Test
    fun `Validation result is valid if amazon key and amazon store`() {
        val expected = APIKeyValidator.ValidationResult.VALID
        val validator = APIKeyValidator()
        val validationResult = validator.validate("amazon_1a2b3c4d5e6f7h", Store.AMAZON)
        assertThat(validationResult).isEqualTo(expected)
    }

    @Test
    fun `Validation result is other platform if apple key`() {
        val expected = APIKeyValidator.ValidationResult.OTHER_PLATFORM
        val validator = APIKeyValidator()
        val validationResult = validator.validate("appl_1a2b3c4d5e6f7h", Store.PLAY_STORE)
        assertThat(validationResult).isEqualTo(expected)
    }
}
