package com.revenuecat.purchases.backup

import android.app.backup.BackupAgentHelper
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.SharedPreferencesBackupHelper
import android.os.ParcelFileDescriptor
import com.revenuecat.purchases.common.SharedPreferencesManager
import com.revenuecat.purchases.common.debugLog

private const val REVENUECAT_PREFS_BACKUP_KEY = "revenuecat_prefs_backup"

class RevenueCatProactiveBackupAgent : BackupAgentHelper() {
    override fun onCreate() {
        SharedPreferencesBackupHelper(this, SharedPreferencesManager.REVENUECAT_PREFS_FILE_NAME).also {
            addHelper(REVENUECAT_PREFS_BACKUP_KEY, it)
        }
    }

    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) {
        debugLog { "RevenueCatProactiveBackupAgent: Initiating backup" }
        super.onBackup(oldState, data, newState)
    }

    override fun onRestore(data: BackupDataInput?, appVersionCode: Long, newState: ParcelFileDescriptor?) {
        debugLog { "RevenueCatProactiveBackupAgent: Initiating restoration" }
        super.onRestore(data, appVersionCode, newState)
    }
}
