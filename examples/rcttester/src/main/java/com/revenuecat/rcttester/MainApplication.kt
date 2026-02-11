package com.revenuecat.rcttester

import android.app.Application
import android.util.Log
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.rcttester.config.SDKConfiguration

class MainApplication : Application() {
    var isSDKConfigured = false
        private set

    private companion object {
        private const val MIN_API_KEY_LENGTH = 10
    }

    override fun onCreate() {
        super.onCreate()

        // Set log level to VERBOSE for debugging
        Purchases.logLevel = LogLevel.VERBOSE

        // Load saved configuration and configure SDK if available
        // Only configure if not already configured
        if (!Purchases.isConfigured) {
            configureFromSavedSettings()
        } else {
            // SDK already configured, mark as configured
            isSDKConfigured = true
        }
    }

    private fun configureFromSavedSettings() {
        val savedConfiguration = SDKConfiguration.load(this) ?: return
        val sanitizedApiKey = sanitizeApiKey(savedConfiguration.apiKey)
        if (!isValidApiKey(sanitizedApiKey)) {
            clearInvalidConfiguration()
            return
        }
        val sanitizedConfig = savedConfiguration.copy(apiKey = sanitizedApiKey)
        try {
            configureSDK(sanitizedConfig)
        } catch (e: IllegalStateException) {
            handleConfigurationError(e)
        }
    }

    private fun sanitizeApiKey(apiKey: String): String {
        return apiKey.trim()
            .replace("\n", "")
            .replace("\r", "")
    }

    private fun isValidApiKey(apiKey: String): Boolean {
        return apiKey.isNotBlank() && apiKey.length > MIN_API_KEY_LENGTH
    }

    private fun clearInvalidConfiguration() {
        Log.w("RCTTester", "Invalid API key found, clearing configuration")
        val prefs = getSharedPreferences("rcttester_config", MODE_PRIVATE)
        prefs.edit().clear().apply()
        isSDKConfigured = false
    }

    private fun handleConfigurationError(e: IllegalStateException) {
        Log.e("RCTTester", "Failed to configure SDK: ${e.message}", e)
        val prefs = getSharedPreferences("rcttester_config", MODE_PRIVATE)
        prefs.edit().clear().apply()
        isSDKConfigured = false
    }

    fun configureSDK(configuration: SDKConfiguration) {
        // Sanitize API key before saving
        val sanitizedApiKey = configuration.apiKey.trim().replace("\n", "").replace("\r", "")
        val sanitizedConfig = configuration.copy(apiKey = sanitizedApiKey)
        sanitizedConfig.save(this)

        // Don't reconfigure if already configured with the same config
        if (Purchases.isConfigured) {
            isSDKConfigured = true
            return
        }

        val purchasesAreCompletedBy = sanitizedConfig.toPurchasesAreCompletedBy()
        val builder = PurchasesConfiguration.Builder(this, sanitizedApiKey)
            .purchasesAreCompletedBy(purchasesAreCompletedBy)

        // Set app user ID if provided, otherwise SDK will generate anonymous ID
        if (sanitizedConfig.appUserID.isNotBlank()) {
            builder.appUserID(sanitizedConfig.appUserID.trim())
        } else {
            builder.appUserID(null)
        }

        Purchases.configure(builder.build())
        isSDKConfigured = true
    }
}
