package com.revenuecat.rcttester.config

import android.content.Context
import com.revenuecat.purchases.PurchasesAreCompletedBy
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Configuration for how the SDK should be set up
 */
@Serializable
data class SDKConfiguration(
    val apiKey: String,
    val appUserID: String,
    val purchasesAreCompletedBy: PurchasesCompletedByType,
) {
    companion object {
        private const val PREFS_NAME = "rcttester_config"
        private const val KEY_CONFIG = "sdk_configuration"
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Loads the configuration from SharedPreferences, or returns null if none exists
         */
        fun load(context: Context): SDKConfiguration? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_CONFIG, null) ?: return null
            return try {
                json.decodeFromString<SDKConfiguration>(jsonString)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Default configuration
         */
        fun default(context: Context): SDKConfiguration {
            return SDKConfiguration(
                apiKey = Constants.getApiKey(context),
                appUserID = "",
                purchasesAreCompletedBy = PurchasesCompletedByType.REVENUECAT,
            )
        }
    }

    /**
     * Saves the configuration to SharedPreferences
     */
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = json.encodeToString(this)
        prefs.edit().putString(KEY_CONFIG, jsonString).apply()
    }

    /**
     * Converts to RevenueCat PurchasesAreCompletedBy enum
     */
    fun toPurchasesAreCompletedBy(): PurchasesAreCompletedBy {
        return when (purchasesAreCompletedBy) {
            PurchasesCompletedByType.REVENUECAT -> PurchasesAreCompletedBy.REVENUECAT
            PurchasesCompletedByType.MY_APP -> PurchasesAreCompletedBy.MY_APP
        }
    }
}

@Serializable
enum class PurchasesCompletedByType {
    REVENUECAT,
    MY_APP;

    val displayName: String
        get() = when (this) {
            REVENUECAT -> ".revenueCat"
            MY_APP -> ".myApp"
        }
}
