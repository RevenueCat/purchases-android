package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.utils.DefaultUrlConnectionFactory
import com.revenuecat.purchases.utils.UrlConnection
import com.revenuecat.purchases.utils.UrlConnectionFactory
import java.io.IOException
import java.util.concurrent.CountDownLatch

/**
 * Checks whether a source is healthy by hitting its `/v1/health/connectivity` endpoint: only a 2xx
 * response counts as healthy; any other response or a connection failure does not. Checks are
 * on-demand only (callers probe after a request to the source has already failed, never proactively).
 *
 * [isHealthy] performs blocking network I/O on the calling thread, so it must always be called from
 * a background thread. Thread-safe: concurrent checks for the same source share a single request
 * (joiners block until the owner's result arrives), and results are cached briefly so a burst of
 * failing requests fires one probe.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class SourceHealthChecker(
    private val urlConnectionFactory: UrlConnectionFactory = DefaultUrlConnectionFactory(),
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {

    private class PendingCheck {
        val latch = CountDownLatch(1)

        @Volatile
        var isHealthy = false
    }

    private data class CachedResult(val timestampMs: Long, val isHealthy: Boolean)

    private val lock = Any()
    private val pendingChecks = mutableMapOf<String, PendingCheck>()
    private val cachedResults = mutableMapOf<String, CachedResult>()

    fun isHealthy(sourceBaseUrl: String): Boolean {
        val healthUrl = healthUrl(sourceBaseUrl)
        var isOwner = false
        val check = synchronized(lock) {
            validCachedResult(healthUrl)?.let { return it }
            pendingChecks.getOrPut(healthUrl) {
                isOwner = true
                PendingCheck()
            }
        }
        return if (isOwner) {
            performOwnedCheck(healthUrl, check)
        } else {
            check.latch.await()
            check.isHealthy
        }
    }

    private fun performOwnedCheck(healthUrl: String, check: PendingCheck): Boolean {
        try {
            check.isHealthy = performCheck(healthUrl)
        } finally {
            synchronized(lock) {
                cachedResults[healthUrl] = CachedResult(dateProvider.now.time, check.isHealthy)
                pendingChecks -= healthUrl
            }
            check.latch.countDown()
        }
        return check.isHealthy
    }

    private fun validCachedResult(healthUrl: String): Boolean? = cachedResults[healthUrl]?.let { cached ->
        cached.isHealthy.takeIf { dateProvider.now.time - cached.timestampMs < RESULT_VALIDITY_MS }
    }

    private fun performCheck(healthUrl: String): Boolean {
        var connection: UrlConnection? = null
        return try {
            connection = urlConnectionFactory.createConnection(healthUrl)
            val responseCode = connection.responseCode
            val isHealthy = responseCode in HEALTHY_RESPONSE_CODES
            verboseLog { "Health check for $healthUrl returned $responseCode (healthy=$isHealthy)" }
            isHealthy
        } catch (e: IOException) {
            verboseLog { "Health check for $healthUrl failed to connect: $e" }
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun healthUrl(sourceBaseUrl: String): String = "${sourceBaseUrl.trimEnd('/')}/$HEALTH_PATH"

    private companion object {
        const val HEALTH_PATH = "v1/health/connectivity"
        const val RESULT_VALIDITY_MS = 10_000L
        val HEALTHY_RESPONSE_CODES = 200..299
    }
}
