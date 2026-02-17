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
    fun `Validation result is simulated store`() {
        assertValidation(
            APIKeyValidator.ValidationResult.SIMULATED_STORE,
            "test_1a2b3c4d5e6f7h",
            Store.PLAY_STORE
        )
        assertValidation(
            APIKeyValidator.ValidationResult.SIMULATED_STORE,
            "test_1a2b3c4d5e6f7h",
            Store.AMAZON
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
        val validationResult = validator.validateAndLog(apiKey, store)
        assertThat(validationResult).isEqualTo(expected)
    }

    @Test
    fun `Redact API key`() {
        val validator = APIKeyValidator()
        assertThat(validator.redactApiKey("test_CtDegh822fag83yggTUVkajsJ")).isEqualTo("test_Ct********ajsJ")

        // Exactly 6 characters after underscore (minimal redactable remainder)
        assertThat(validator.redactApiKey("test_123456")).isEqualTo("test_12********3456")
        assertThat(validator.redactApiKey("api_abcdef")).isEqualTo("api_ab********cdef")
        assertThat(validator.redactApiKey("_abcdef")).isEqualTo("_ab********cdef")
        assertThat(validator.redactApiKey("a_123456")).isEqualTo("a_12********3456")

        // Short remainder: <6 chars → should NOT redact
        assertThat(validator.redactApiKey("test_12345")).isEqualTo("test_12345")
        assertThat(validator.redactApiKey("_abc")).isEqualTo("_abc")
        assertThat(validator.redactApiKey("test_")).isEqualTo("test_")

        // Multiple underscores: only the first underscore counts
        assertThat(validator.redactApiKey("test_abcd_efghijkl")).isEqualTo("test_ab********ijkl")

        // Empty string and single underscore → should NOT crash and should NOT redact
        assertThat(validator.redactApiKey("")).isEqualTo("")
        assertThat(validator.redactApiKey("_")).isEqualTo("_")

        // Legacy API keys without a prefix
        assertThat(validator.redactApiKey("g9h2g7q36fg")).isEqualTo("g9********36fg")
        assertThat(validator.redactApiKey("ab34e")).isEqualTo("ab34e")
    }
}
