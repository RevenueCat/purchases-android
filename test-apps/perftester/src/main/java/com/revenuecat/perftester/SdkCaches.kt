package com.revenuecat.perftester

import android.content.Context
import java.io.File

/**
 * Inspects and clears the SDK's on-device caches. Locations mirror the SDK:
 * - Prefs file: RevenueCatBackupAgent.REVENUECAT_PREFS_FILE_NAME
 * - Offerings key: DeviceCache offeringsResponseCacheKey ("com.revenuecat.purchases.<apiKey>.offeringsResponse")
 * - Remote config + blobs: RemoteConfigDiskCache/RemoteConfigBlobStore under noBackupFilesDir/RevenueCat
 */
internal object SdkCaches {

    private const val RC_PREFS_FILE = "com_revenuecat_purchases_preferences"

    fun hadOfferingsCache(context: Context, apiKey: String): Boolean =
        context.getSharedPreferences(RC_PREFS_FILE, Context.MODE_PRIVATE)
            .contains("com.revenuecat.purchases.$apiKey.offeringsResponse")

    fun hadRemoteConfigCache(context: Context): Boolean =
        File(File(context.noBackupFilesDir, "RevenueCat"), "remote_config")
            .listFiles()?.isNotEmpty() == true

    fun hadBlobsCache(context: Context): Boolean =
        File(File(context.noBackupFilesDir, "RevenueCat"), "blobs")
            .listFiles()?.isNotEmpty() == true

    fun clearAll(context: Context) {
        clearPrefs(context, RC_PREFS_FILE)
        clearPrefs(context, "${context.packageName}_preferences_etags")
        clearPrefs(context, "com_revenuecat_purchases_${context.packageName}_preferences_diagnostics")
        File(context.filesDir, "RevenueCat").deleteRecursively()
        File(context.noBackupFilesDir, "RevenueCat").deleteRecursively()
    }

    private fun clearPrefs(context: Context, name: String) {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
    }
}
