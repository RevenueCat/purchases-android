package com.revenuecat.purchases.common

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Provides an instance of SharedPreferences to be used by the RevenueCat SDK.
 * It handles migration from legacy shared preferences to a dedicated RevenueCat-specific preferences file.
 * The migration is performed only once, and it ensures that no data is lost during the process
 */
internal class SharedPreferencesManager(
    context: Context,
    private val revenueCatSharedPreferences: SharedPreferences = context.getSharedPreferences(
        REVENUECAT_PREFS_FILE_NAME,
        Context.MODE_PRIVATE,
    ),
    private val legacySharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context),
) {

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val REVENUECAT_PREFS_FILE_NAME = "com_revenuecat_purchases_preferences"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SHARED_PREFERENCES_PREFIX = "com.revenuecat.purchases."
    }

    private val migrationLock = ReentrantLock()

    /**
     * Gets the appropriate shared preferences, performing migration if needed
     */
    fun getSharedPreferences(): SharedPreferences {
        migrationLock.withLock {
            if (shouldPerformMigration()) {
                performMigration()
            }
        }
        return revenueCatSharedPreferences
    }

    /**
     * Checks if migration should be performed by checking if RevenueCat preferences are empty
     * and legacy preferences contain RevenueCat data
     */
    private fun shouldPerformMigration(): Boolean {
        val revenueCatPrefs = revenueCatSharedPreferences
        val legacyPrefs = legacySharedPreferences

        // If RevenueCat preferences already have data, no migration needed
        if (revenueCatPrefs.all.isNotEmpty()) {
            return false
        }

        // Check if legacy preferences contain any RevenueCat data
        return legacyPrefs.all.keys.any { key ->
            key.startsWith(SHARED_PREFERENCES_PREFIX)
        }
    }

    /**
     * Performs the migration from legacy shared preferences to RevenueCat-specific ones
     */
    private fun performMigration() {
        try {
            log(
                LogIntent.DEBUG,
            ) { "Starting shared preferences migration from legacy to RevenueCat-specific preferences" }

            val revenueCatKeys = getRevenueCatKeysToMigrate()

            val legacyPrefs = legacySharedPreferences
            val revenueCatPrefs = revenueCatSharedPreferences
            revenueCatPrefs.edit(commit = true) {
                var migratedCount = 0
                for (key in revenueCatKeys) {
                    if (migratePreferenceValue(legacyPrefs, this, key)) {
                        migratedCount++
                    }
                }
            }
        } catch (e: java.lang.IllegalStateException) {
            errorLog(e) { "Failed to perform shared preferences migration due to invalid state" }
        } catch (e: java.io.IOException) {
            errorLog(e) { "Failed to perform shared preferences migration due to IO error" }
        }
    }

    private fun getRevenueCatKeysToMigrate(): List<String> {
        val legacyPrefs = legacySharedPreferences
        val revenueCatKeys = legacyPrefs.all.keys.filter { key ->
            key.startsWith(SHARED_PREFERENCES_PREFIX)
        }

        log(LogIntent.DEBUG) { "Found ${revenueCatKeys.size} RevenueCat keys to migrate: $revenueCatKeys" }
        return revenueCatKeys
    }

    private fun migratePreferenceValue(
        legacyPrefs: SharedPreferences,
        editor: SharedPreferences.Editor,
        key: String,
    ): Boolean {
        return try {
            val value = legacyPrefs.all[key]
            when (value) {
                is String -> {
                    editor.putString(key, value)
                    true
                }
                is Boolean -> {
                    editor.putBoolean(key, value)
                    true
                }
                is Int -> {
                    editor.putInt(key, value)
                    true
                }
                is Long -> {
                    editor.putLong(key, value)
                    true
                }
                is Float -> {
                    editor.putFloat(key, value)
                    true
                }
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    editor.putStringSet(key, value as? Set<String> ?: emptySet())
                    true
                }
                else -> {
                    log(
                        LogIntent.WARNING,
                    ) { "Unknown preference type for key $key: ${value?.javaClass?.simpleName}" }
                    false
                }
            }
        } catch (e: java.lang.ClassCastException) {
            errorLog(e) { "Failed to migrate preference with key due to type casting: $key" }
            false
        }
    }
}
