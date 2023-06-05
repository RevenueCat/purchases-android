package com.revenuecat.purchases

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.strings.ConfigureStrings

private const val GOOGLE_API_KEY_PREFIX = "goog_"
private const val AMAZON_API_KEY_PREFIX = "amzn_"

internal class APIKeyValidator {

    private enum class APIKeyPlatform {
        GOOGLE,
        AMAZON,
        LEGACY,
        OTHER_PLATFORM,
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    enum class ValidationResult {
        VALID,
        GOOGLE_KEY_AMAZON_STORE,
        AMAZON_KEY_GOOGLE_STORE,
        LEGACY,
        OTHER_PLATFORM,
    }

    fun validateAndLog(apiKey: String, configuredStore: Store) {
        when (validate(apiKey, configuredStore)) {
            ValidationResult.AMAZON_KEY_GOOGLE_STORE -> errorLog(ConfigureStrings.AMAZON_API_KEY_GOOGLE_STORE)
            ValidationResult.GOOGLE_KEY_AMAZON_STORE -> errorLog(ConfigureStrings.GOOGLE_API_KEY_AMAZON_STORE)
            ValidationResult.LEGACY -> debugLog(ConfigureStrings.LEGACY_API_KEY)
            ValidationResult.OTHER_PLATFORM -> errorLog(ConfigureStrings.INVALID_API_KEY)
            ValidationResult.VALID -> {}
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun validate(apiKey: String, configuredStore: Store): ValidationResult {
        val apiKeyPlatform = getApiKeyPlatform(apiKey)
        return when {
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

    private fun getApiKeyPlatform(apiKey: String): APIKeyPlatform {
        return when {
            apiKey.startsWith(GOOGLE_API_KEY_PREFIX) -> APIKeyPlatform.GOOGLE
            apiKey.startsWith(AMAZON_API_KEY_PREFIX) -> APIKeyPlatform.AMAZON
            !apiKey.contains('_') -> APIKeyPlatform.LEGACY
            else -> APIKeyPlatform.OTHER_PLATFORM
        }
    }
}
