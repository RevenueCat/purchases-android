package com.revenuecat.purchases.backup

import android.app.backup.BackupAgentHelper
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.SharedPreferencesBackupHelper
import android.os.ParcelFileDescriptor
import com.revenuecat.purchases.common.debugLog

private const val REVENUECAT_PREFS_BACKUP_KEY = "revenuecat_prefs_backup"

/**
 * A BackupAgent that proactively backs up RevenueCat's SharedPreferences. You may use this by adding the following
 * to your AndroidManifest.xml within the `<application>` tag:
 * ```
 * android:backupAgent="com.revenuecat.purchases.backup.RevenueCatBackupAgent"
 * ```
 * This will backup the SharedPreferences file used by the RevenueCat SDK, allowing it to keep the same user as
 * was previously used in the same or different device with the same Google account, removing the need for users to
 * restore purchases.
 *
 * Some important notes:
 * - This backup may not work on all devices, as it's ultimately controlled by the Android system and the user settings.
 * See https://developer.android.com/identity/data/keyvaluebackup for more details on how key-value backup works
 * and how to test it.
 * - Setting the backup agent in your AndroidManifest would disable auto backup for your app, if it was enabled.
 * If you want to use auto backup to also backup your app's data or have your own Backup Agent to backup some of your
 * files, please make sure you add the SharedPreferences file `com_revenuecat_purchases_preferences` to your auto backup
 * configuration. See https://developer.android.com/identity/data/autobackup.
 */
public class RevenueCatBackupAgent : BackupAgentHelper() {
    public companion object {
        public const val REVENUECAT_PREFS_FILE_NAME: String = "com_revenuecat_purchases_preferences"
    }

    override fun onCreate() {
        SharedPreferencesBackupHelper(this, REVENUECAT_PREFS_FILE_NAME).also {
            addHelper(REVENUECAT_PREFS_BACKUP_KEY, it)
        }
    }

    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) {
        debugLog { "RevenueCatBackupAgent: Initiating backup" }
        super.onBackup(oldState, data, newState)
    }

    override fun onRestore(data: BackupDataInput?, appVersionCode: Long, newState: ParcelFileDescriptor?) {
        debugLog { "RevenueCatBackupAgent: Initiating restoration" }
        super.onRestore(data, appVersionCode, newState)
    }
}
