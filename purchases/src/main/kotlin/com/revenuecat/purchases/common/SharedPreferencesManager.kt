package com.revenuecat.purchases.common

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.revenuecat.purchases.backup.RevenueCatBackupAgent

/**
 * Provides an instance of SharedPreferences to be used by the RevenueCat SDK.
 * It handles migration from legacy shared preferences to a dedicated RevenueCat-specific preferences file.
 * The migration is performed only once, and it ensures that no data is lost during the process
 */
internal class SharedPreferencesManager(
    context: Context,
    private val revenueCatSharedPreferences: SharedPreferences = context.getSharedPreferences(
        RevenueCatBackupAgent.REVENUECAT_PREFS_FILE_NAME,
        Context.MODE_PRIVATE,
    ),
    private val legacySharedPreferences: Lazy<SharedPreferences> = lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    },
) {

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SHARED_PREFERENCES_PREFIX = "com.revenuecat.purchases."

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val EXPECTED_VERSION_KEY = "com.revenuecat.purchases.shared_preferences_version"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val EXPECTED_VERSION = 1
    }

    /**
     * Gets the appropriate shared preferences, performing migration if needed
     */
    fun getSharedPreferences(): SharedPreferences {
        synchronized(this) { ensureMigrated() }
        return revenueCatSharedPreferences
    }

    private fun ensureMigrated() {
        val alreadyHasVersion = hasRevenueCatVersion()
        if (!alreadyHasVersion) {
            // Read the legacy preferences only once. `.all` forces a full synchronous load of the backing XML
            // file, which is expensive when it holds a large value (e.g. the cached offerings response), so we
            // avoid touching it more than necessary on this (typically main-thread) startup path.
            val legacyValues = legacySharedPreferences.value.all
            val revenueCatKeys = legacyValues.keys.filter { it.startsWith(SHARED_PREFERENCES_PREFIX) }
            if (revenueCatKeys.isNotEmpty()) {
                performMigration(legacyValues, revenueCatKeys)
            }
            updateSharedPreferencesVersion()
        }
    }

    /**
     * Performs the migration from legacy shared preferences to RevenueCat-specific ones
     */
    private fun performMigration(legacyValues: Map<String, *>, revenueCatKeys: List<String>) {
        log(
            LogIntent.DEBUG,
        ) { "Starting shared preferences migration from legacy to RevenueCat-specific preferences" }
        log(LogIntent.DEBUG) { "Found ${revenueCatKeys.size} RevenueCat keys to migrate: $revenueCatKeys" }

        val revenueCatPrefs = revenueCatSharedPreferences
        revenueCatPrefs.edit {
            for (key in revenueCatKeys) {
                migratePreferenceValue(legacyValues[key], this, key)
            }
        }

        log(
            LogIntent.DEBUG,
        ) { "Finished shared preferences migration from legacy to RevenueCat-specific preferences" }
    }

    private fun migratePreferenceValue(
        value: Any?,
        editor: SharedPreferences.Editor,
        key: String,
    ): Boolean {
        return try {
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

    private fun hasRevenueCatVersion(): Boolean {
        return revenueCatSharedPreferences.contains(EXPECTED_VERSION_KEY)
    }

    private fun updateSharedPreferencesVersion() {
        revenueCatSharedPreferences.edit {
            putInt(EXPECTED_VERSION_KEY, EXPECTED_VERSION)
        }
    }
}
