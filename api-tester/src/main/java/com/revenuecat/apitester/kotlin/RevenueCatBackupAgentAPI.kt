package com.revenuecat.apitester.kotlin

import android.app.backup.BackupAgentHelper
import com.revenuecat.purchases.backup.RevenueCatBackupAgent

@Suppress("unused", "UNUSED_VARIABLE")
private class RevenueCatBackupAgentAPI {
    fun check(agent: RevenueCatBackupAgent) {
        val backupAgentHelper: BackupAgentHelper = agent
    }
}
