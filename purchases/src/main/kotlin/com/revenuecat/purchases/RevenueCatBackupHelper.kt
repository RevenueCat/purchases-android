package com.revenuecat.purchases

import android.app.backup.BackupAgentHelper
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.SharedPreferencesBackupHelper
import android.os.Build
import android.os.ParcelFileDescriptor
import android.preference.PreferenceManager
import android.util.Log

private const val PREFS_BACKUP_KEY = "prefs"

class RevenueCatBackupHelper: BackupAgentHelper() {
    override fun onCreate() {
        super.onCreate()
        Log.e("BACKUP", "BACKUP RevenueCatBackupHelper")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val preferencesName = PreferenceManager.getDefaultSharedPreferencesName(applicationContext)
            SharedPreferencesBackupHelper(this, preferencesName).also {
                addHelper(PREFS_BACKUP_KEY, it)
            }
        }
    }

    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) {
        super.onBackup(oldState, data, newState)
        Log.e("BACKUP", "onBackup called in RevenueCatBackupHelper")
    }

    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) {
        super.onRestore(data, appVersionCode, newState)
        Log.e("BACKUP", "onRestore called in RevenueCatBackupHelper")
    }
}
