package com.revenuecat.purchases.common.networking

import android.content.Context
import android.net.ConnectivityManager
import com.revenuecat.purchases.common.verboseLog

/**
 * Answers whether the device itself has any network at all, from the OS's active-network signal. A
 * cheap local call, no network I/O.
 *
 * Deliberately does not consider [android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED]: that flag
 * comes from the OS's own captive-portal probe against Google-owned hosts, so a VPN or a network that
 * filters those hosts reports "not validated" while our API hosts are perfectly reachable.
 */
internal class DeviceConnectivityChecker(context: Context) {

    private val applicationContext = context.applicationContext

    /**
     * True only when the OS positively reports no active network. Any uncertainty (missing
     * ACCESS_NETWORK_STATE permission, ConnectivityManager unavailable, framework exception) is
     * false, so callers fail open to their normal behavior.
     */
    @Suppress("TooGenericExceptionCaught")
    fun isDeviceOffline(): Boolean {
        return try {
            val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager ?: return false
            connectivityManager.activeNetwork == null
        } catch (e: RuntimeException) {
            verboseLog { "Could not determine device connectivity, assuming online: $e" }
            false
        }
    }
}
