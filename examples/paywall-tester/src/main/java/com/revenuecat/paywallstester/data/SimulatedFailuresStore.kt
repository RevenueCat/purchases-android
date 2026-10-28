package com.revenuecat.paywallstester.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Persists the simulated failures mode; it is applied the next time the app starts. */
internal class SimulatedFailuresStore(
    private val sharedPreferences: SharedPreferences,
) {
    internal companion object {
        private const val SHARED_PREFERENCES_NAME = "com.revenuecat.paywallstester"
        private const val KEY_MODE = "com.revenuecat.paywallstester.simulated_failures_mode"
    }

    constructor(context: Context) : this(context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE))

    fun set(mode: SimulatedFailuresMode) {
        sharedPreferences.edit { putString(KEY_MODE, mode.name) }
    }

    fun get(): SimulatedFailuresMode = SimulatedFailuresMode.fromName(sharedPreferences.getString(KEY_MODE, null))
}
