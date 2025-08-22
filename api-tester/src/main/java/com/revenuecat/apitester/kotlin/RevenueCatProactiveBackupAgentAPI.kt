package com.revenuecat.apitester.kotlin

import android.app.backup.BackupAgentHelper
import com.revenuecat.purchases.backup.RevenueCatProactiveBackupAgent

@Suppress("unused", "UNUSED_VARIABLE")
private class RevenueCatProactiveBackupAgentAPI {
    fun check(agent: RevenueCatProactiveBackupAgent) {
        val backupAgentHelper: BackupAgentHelper = agent
    }
}
