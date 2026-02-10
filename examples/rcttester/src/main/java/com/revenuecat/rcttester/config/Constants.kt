package com.revenuecat.rcttester.config

import android.content.Context

object Constants {
    /**
     * Gets the API key from BuildConfig or returns empty string
     * Can be set via buildConfigField in build.gradle.kts if needed
     */
    fun getApiKey(context: Context): String {
        // Return empty string - user will need to enter it in the app
        // Can be set via buildConfigField in build.gradle.kts if needed
        return ""
    }
}
