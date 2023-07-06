package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class APIKeyValidatorTest {

    @Test
    fun `Validation result is google key but amazon store if API key is from google but store is amazon`() {
        assertValidation(
            APIKeyValidator.ValidationResult.GOOGLE_KEY_AMAZON_STORE,
            "goog_1a2b3c4d5e6f7h",
            Store.AMAZON
        )
    }

    @Test
    fun `Validation result is amazon key but google store if API key is from amazon but store is google`() {
        assertValidation(
            APIKeyValidator.ValidationResult.AMAZON_KEY_GOOGLE_STORE,
            "amzn_1a2b3c4d5e6f7h",
            Store.PLAY_STORE
        )
    }

    @Test
    fun `Validation result is legacy`() {
        assertValidation(APIKeyValidator.ValidationResult.LEGACY, "1a2b3c4d5e6f7h", Store.PLAY_STORE)
    }

    @Test
    fun `Validation result is valid if google key and google store`() {
        assertValidation(APIKeyValidator.ValidationResult.VALID, "goog_1a2b3c4d5e6f7h", Store.PLAY_STORE)
    }

    @Test
    fun `Validation result is valid if amazon key and amazon store`() {
        assertValidation(APIKeyValidator.ValidationResult.VALID, "amzn_1a2b3c4d5e6f7h", Store.AMAZON)
    }

    @Test
    fun `Validation result is other platform if apple key`() {
        assertValidation(APIKeyValidator.ValidationResult.OTHER_PLATFORM, "appl_1a2b3c4d5e6f7h", Store.PLAY_STORE)
    }

    private fun assertValidation(expected: APIKeyValidator.ValidationResult, apiKey: String, store: Store) {
        val validator = APIKeyValidator()
        val validationResult = validator.validate(apiKey, store)
        assertThat(validationResult).isEqualTo(expected)
    }
}
