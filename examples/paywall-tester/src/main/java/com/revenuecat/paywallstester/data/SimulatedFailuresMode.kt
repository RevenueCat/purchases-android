package com.revenuecat.paywallstester.data

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.SimulatedHostFailures

/** Which API hosts the SDK should pretend are failing, so API source failover can be exercised by hand. */
internal enum class SimulatedFailuresMode(val label: String) {
    NONE("None"),
    PRIMARY_UNREACHABLE("Primary source unreachable (fails over to backup)"),
    PRIMARY_SERVER_ERROR("Primary source 5xx but healthy (no failover)"),
    ALL_UNREACHABLE("All sources unreachable (static fallback only)"),
    ;

    /** Never null: an empty instance keeps the SDK hook armed so the mode can change mid-session. */
    @OptIn(InternalRevenueCatAPI::class)
    fun hostFailures(): SimulatedHostFailures = when (this) {
        NONE -> SimulatedHostFailures(serverErrorHosts = emptySet(), unreachableHosts = emptySet())
        PRIMARY_UNREACHABLE -> SimulatedHostFailures(
            serverErrorHosts = emptySet(),
            unreachableHosts = setOf(PRIMARY_HOST),
        )
        PRIMARY_SERVER_ERROR -> SimulatedHostFailures(
            serverErrorHosts = setOf(PRIMARY_HOST),
            unreachableHosts = emptySet(),
        )
        ALL_UNREACHABLE -> SimulatedHostFailures(
            serverErrorHosts = emptySet(),
            unreachableHosts = setOf(PRIMARY_HOST, BACKUP_HOST),
        )
    }

    companion object {
        private const val PRIMARY_HOST = "api.revenuecat.com"
        private const val BACKUP_HOST = "api.rc-backup.com"

        fun fromName(value: String?): SimulatedFailuresMode =
            value?.let { name -> values().firstOrNull { it.name == name } } ?: NONE
    }
}
