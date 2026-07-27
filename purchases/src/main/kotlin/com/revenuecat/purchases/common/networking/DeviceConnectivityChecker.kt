package com.revenuecat.purchases.common.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.revenuecat.purchases.common.verboseLog

/**
 * Answers whether the device itself has working connectivity, using the OS's own validated-network
 * signal ([NetworkCapabilities.NET_CAPABILITY_VALIDATED]), which is false while offline or behind a
 * captive portal. A cheap local call, no network I/O.
 */
internal class DeviceConnectivityChecker(context: Context) {

    private val applicationContext = context.applicationContext

    /**
     * True only when the OS positively reports no validated network. Any uncertainty (missing
     * ACCESS_NETWORK_STATE permission, ConnectivityManager unavailable, framework exception) is
     * false, so callers fail open to their normal behavior.
     */
    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    fun isDeviceOffline(): Boolean {
        return try {
            val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager ?: return false
            val activeNetwork = connectivityManager.activeNetwork ?: return true
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return true
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: RuntimeException) {
            verboseLog { "Could not determine device connectivity, assuming online: $e" }
            false
        }
    }
}
