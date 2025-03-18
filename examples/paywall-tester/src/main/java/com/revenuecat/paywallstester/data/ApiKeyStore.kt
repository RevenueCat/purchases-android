package com.revenuecat.paywallstester.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.revenuecat.paywallstester.Constants

internal class ApiKeyStore(
    private val sharedPreferences: SharedPreferences,
) {
    internal companion object {
        private const val SHARED_PREFERENCES_NAME = "com.revenuecat.paywallstester"
        private const val KEY_LAST_USED_API_KEY = "com.revenuecat.paywallstester.last_used_api_key"
    }

    constructor(context: Context) : this(context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE))

    fun setLastUsedApiKey(apiKey: String) {
        sharedPreferences.edit {
            putString(KEY_LAST_USED_API_KEY, apiKey)
        }
    }

    fun getLastUsedApiKey(): String = sharedPreferences.getString(KEY_LAST_USED_API_KEY, Constants.GOOGLE_API_KEY_A)!!
}
