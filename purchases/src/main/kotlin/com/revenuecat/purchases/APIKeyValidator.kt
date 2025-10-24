package com.revenuecat.purchases

import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.strings.ConfigureStrings

private const val GOOGLE_API_KEY_PREFIX = "goog_"
private const val AMAZON_API_KEY_PREFIX = "amzn_"
private const val TEST_API_KEY_PREFIX = "test_"

internal class APIKeyValidator {

    private enum class APIKeyPlatform {
        GOOGLE,
        AMAZON,
        LEGACY,
        TEST,
        OTHER_PLATFORM,
    }

    enum class ValidationResult {
        VALID,
        GOOGLE_KEY_AMAZON_STORE,
        AMAZON_KEY_GOOGLE_STORE,
        LEGACY,
        SIMULATED_STORE,
        OTHER_PLATFORM,
    }

    fun validateAndLog(apiKey: String, configuredStore: Store): ValidationResult {
        val validationResult = validate(apiKey, configuredStore)
        logValidationResult(validationResult)
        return validationResult
    }

    private fun validate(apiKey: String, configuredStore: Store): ValidationResult {
        val apiKeyPlatform = getApiKeyPlatform(apiKey)
        return when {
            apiKeyPlatform == APIKeyPlatform.TEST -> ValidationResult.SIMULATED_STORE
            apiKeyPlatform == APIKeyPlatform.GOOGLE && configuredStore == Store.PLAY_STORE -> ValidationResult.VALID
            apiKeyPlatform == APIKeyPlatform.AMAZON && configuredStore == Store.AMAZON -> ValidationResult.VALID
            apiKeyPlatform == APIKeyPlatform.GOOGLE && configuredStore == Store.AMAZON -> {
                ValidationResult.GOOGLE_KEY_AMAZON_STORE
            }
            apiKeyPlatform == APIKeyPlatform.AMAZON && configuredStore == Store.PLAY_STORE -> {
                ValidationResult.AMAZON_KEY_GOOGLE_STORE
            }
            apiKeyPlatform == APIKeyPlatform.LEGACY -> ValidationResult.LEGACY
            apiKeyPlatform == APIKeyPlatform.OTHER_PLATFORM -> ValidationResult.OTHER_PLATFORM
            else -> ValidationResult.OTHER_PLATFORM
        }
    }

    private fun logValidationResult(validationResult: ValidationResult) {
        when (validationResult) {
            ValidationResult.AMAZON_KEY_GOOGLE_STORE -> errorLog { ConfigureStrings.AMAZON_API_KEY_GOOGLE_STORE }
            ValidationResult.GOOGLE_KEY_AMAZON_STORE -> errorLog { ConfigureStrings.GOOGLE_API_KEY_AMAZON_STORE }
            ValidationResult.LEGACY -> debugLog { ConfigureStrings.LEGACY_API_KEY }
            ValidationResult.OTHER_PLATFORM -> errorLog { ConfigureStrings.INVALID_API_KEY }
            ValidationResult.SIMULATED_STORE -> warnLog { ConfigureStrings.SIMULATED_STORE_API_KEY }
            ValidationResult.VALID -> {}
        }
    }

    private fun getApiKeyPlatform(apiKey: String): APIKeyPlatform {
        return when {
            apiKey.startsWith(GOOGLE_API_KEY_PREFIX) -> APIKeyPlatform.GOOGLE
            apiKey.startsWith(AMAZON_API_KEY_PREFIX) -> APIKeyPlatform.AMAZON
            apiKey.startsWith(TEST_API_KEY_PREFIX) -> APIKeyPlatform.TEST
            !apiKey.contains('_') -> APIKeyPlatform.LEGACY
            else -> APIKeyPlatform.OTHER_PLATFORM
        }
    }
}
