package com.revenuecat.purchases.perf

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Looper
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import okhttp3.mockwebserver.MockWebServer
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class CycleResult(
    val elapsedMs: Long,
    val offeringsCount: Int,
    val requestPaths: List<String>,
    val error: PurchasesError?,
)

@OptIn(InternalRevenueCatAPI::class)
class PerfHarness(
    private val context: Context,
    private val server: MockWebServer,
    private val apiKey: String = "test_perfHarnessKey",
) {
    fun runCycle(useWorkflows: Boolean, cold: Boolean): CycleResult {
        // Robolectric does not auto-grant manifest permissions; configure() requires INTERNET.
        (context.applicationContext as? Application)?.let {
            shadowOf(it).grantPermissions(Manifest.permission.INTERNET)
        }
        Purchases.backingFieldSharedInstance?.close()
        if (cold) wipeDisk()
        Purchases.proxyURL = server.url("/").toUrl()

        val builder = PurchasesConfiguration.Builder(context, apiKey).diagnosticsEnabled(false)
        if (useWorkflows) builder.dangerousSettings(DangerousSettings.forWorkflows())
        Purchases.configure(builder.build())

        val latch = CountDownLatch(1)
        var offerings: Offerings? = null
        var error: PurchasesError? = null
        val start = System.nanoTime()
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error = it; latch.countDown() },
            onSuccess = { offerings = it; latch.countDown() },
        )
        // The SDK posts the final callback to the main Looper. Robolectric's PAUSED looper never
        // auto-drains it, and this thread IS the main thread, so a plain latch.await() deadlocks.
        // Pump the looper between short waits so background network work can post its callback.
        val deadlineMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)
        while (latch.count > 0L && System.currentTimeMillis() < deadlineMs) {
            shadowOf(Looper.getMainLooper()).idle()
            latch.await(POLL_MS, TimeUnit.MILLISECONDS)
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        check(latch.count == 0L) { "getOfferings timed out after $TIMEOUT_SECONDS s" }

        // The workflow /config sync is fired on a background thread and is NOT awaited by
        // getOfferings, so it may still be in flight when the callback returns. Drain with a
        // bounded grace wait — collect requests until none arrives within DRAIN_GRACE_MS, capped
        // by DRAIN_MAX_MS — so the async config request is captured deterministically rather than
        // racing a zero-timeout drain. (takeRequest blocks this thread, but the SDK's network I/O
        // runs on background executors, so the in-flight request still lands.)
        val paths = mutableListOf<String>()
        val drainDeadlineMs = System.currentTimeMillis() + DRAIN_MAX_MS
        while (System.currentTimeMillis() < drainDeadlineMs) {
            shadowOf(Looper.getMainLooper()).idle()
            val req = server.takeRequest(DRAIN_GRACE_MS, TimeUnit.MILLISECONDS) ?: break
            req.path?.let { paths.add(it) }
        }
        return CycleResult(elapsedMs, offerings?.all?.size ?: 0, paths, error)
    }

    private fun wipeDisk() {
        listOf(
            "com_revenuecat_purchases_preferences",
            "${context.packageName}_preferences_etags",
            "com_revenuecat_purchases_${context.packageName}_preferences_diagnostics",
            "com.revenuecat.purchases.transaction_metadata.$apiKey",
        ).forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
        File(context.noBackupFilesDir, "RevenueCat").deleteRecursively()
    }

    private companion object {
        const val TIMEOUT_SECONDS = 15L
        const val POLL_MS = 50L
        const val DRAIN_GRACE_MS = 400L
        const val DRAIN_MAX_MS = 3000L
    }
}
