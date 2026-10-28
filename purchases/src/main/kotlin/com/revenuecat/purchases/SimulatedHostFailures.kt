package com.revenuecat.purchases

import com.revenuecat.purchases.common.networking.Endpoint
import dev.drewhamilton.poko.Poko
import java.net.URL

/**
 * API hosts the SDK should treat as failing, so API source failover can be exercised by hand from a
 * test app. See [Purchases.internalSimulatedHostFailures]; nothing sets it in production.
 */
@InternalRevenueCatAPI
@Poko
public class SimulatedHostFailures(
    /** Requests to these hosts get a server error, but their health checks pass, so the SDK doesn't fail over. */
    public val serverErrorHosts: Set<String>,
    /** Requests to these hosts get a server error and their health checks fail, so the SDK fails over. */
    public val unreachableHosts: Set<String>,
) {

    internal companion object {
        /** A strategy that re-reads [current] on every request, so changes apply mid-session. */
        fun strategy(
            current: () -> SimulatedHostFailures?,
        ): ForceServerErrorStrategy = object : ForceServerErrorStrategy {
            override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean {
                val failures = current() ?: return false
                return baseURL.host in failures.serverErrorHosts || baseURL.host in failures.unreachableHosts
            }

            override fun shouldForceConnectionFailure(url: String): Boolean {
                val failures = current() ?: return false
                return URL(url).host in failures.unreachableHosts
            }
        }
    }
}
