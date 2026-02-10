package com.revenuecat.rcttester

import android.app.Application
import com.revenuecat.rcttester.config.SDKConfiguration
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class MainApplication : Application() {
    var isSDKConfigured = false
        private set

    override fun onCreate() {
        super.onCreate()

        // Set log level to VERBOSE for debugging
        Purchases.logLevel = LogLevel.VERBOSE

        // Load saved configuration and configure SDK if available
        // Only configure if not already configured
        if (!Purchases.isConfigured) {
            val savedConfiguration = SDKConfiguration.load(this)
            if (savedConfiguration != null) {
                // Sanitize API key - remove newlines and trim
                val sanitizedApiKey = savedConfiguration.apiKey.trim().replace("\n", "").replace("\r", "")
                if (sanitizedApiKey.isNotBlank() && sanitizedApiKey.length > 10) {
                    // Only configure if API key looks valid (at least 10 chars)
                    val sanitizedConfig = savedConfiguration.copy(apiKey = sanitizedApiKey)
                    try {
                        configureSDK(sanitizedConfig)
                    } catch (e: Exception) {
                        // If configuration fails, clear the saved config
                        android.util.Log.e("RCTTester", "Failed to configure SDK: ${e.message}", e)
                        // Clear invalid configuration
                        val prefs = getSharedPreferences("rcttester_config", MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        isSDKConfigured = false
                    }
                } else {
                    // Invalid API key, clear it
                    android.util.Log.w("RCTTester", "Invalid API key found, clearing configuration")
                    val prefs = getSharedPreferences("rcttester_config", MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    isSDKConfigured = false
                }
            }
        } else {
            // SDK already configured, mark as configured
            isSDKConfigured = true
        }
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

        val builder = PurchasesConfiguration.Builder(this, sanitizedApiKey)
            .purchasesAreCompletedBy(sanitizedConfig.toPurchasesAreCompletedBy())

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
