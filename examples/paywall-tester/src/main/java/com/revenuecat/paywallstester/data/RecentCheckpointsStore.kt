package com.revenuecat.paywallstester.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

internal class RecentCheckpointsStore(
    private val sharedPreferences: SharedPreferences,
) {
    internal companion object {
        private const val SHARED_PREFERENCES_NAME = "com.revenuecat.paywallstester"
        private const val KEY_RECENT_CHECKPOINTS = "com.revenuecat.paywallstester.recent_checkpoints"
        private const val MAX_RECENTS = 10
        private const val SEPARATOR = "\n"
        private val DEFAULT_RECENTS = listOf("test_checkpoint", "unknown_checkpoint", "error_checkpoint")
    }

    constructor(context: Context) : this(context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE))

    fun recents(): List<String> =
        sharedPreferences.getString(KEY_RECENT_CHECKPOINTS, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: DEFAULT_RECENTS

    fun recordUse(identifier: String): List<String> {
        val updatedRecents = (listOf(identifier) + (recents() - identifier)).take(MAX_RECENTS)
        sharedPreferences.edit {
            putString(KEY_RECENT_CHECKPOINTS, updatedRecents.joinToString(SEPARATOR))
        }
        return updatedRecents
    }
}
