package com.revenuecat.purchases.benchmark

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.backup.RevenueCatBackupAgent
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.resetSingleton
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Measures the wall-clock time of the full [Purchases.getOfferings] API on a cold install, comparing
 * remote config disabled vs enabled.
 *
 * This is an informational benchmark, not a pass/fail gate: each scenario runs [MEASURED_ITERATIONS]
 * cold iterations (after [WARMUP_ITERATIONS] discarded warmups) and logs median/p90/min/max/mean to
 * logcat under [TAG]. It self-skips unless a `test_`-prefixed key is provided via
 * `TEST_STORE_REVENUECAT_API_KEY`, which routes product resolution through the simulated store so no
 * Google Play Billing connection is required.
 *
 * Remote config does not reroute the offerings data itself; enabling it (via [DangerousSettings.forWorkflows])
 * makes getOfferings additionally sync `/v1/config` and wait on the paywall-config readiness gate, so the
 * enabled scenario captures that config-layer overhead. For that number to be meaningful the key must have
 * its workflows/ui_config topics provisioned.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(InternalRevenueCatAPI::class)
@Ignore("Benchmark: run on demand only. Remove this @Ignore (or run the method explicitly) to measure.")
internal class GetOfferingsBenchmarkTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext
    private val apiKey get() = Constants.testStoreApiKey

    @Before
    fun setUp() {
        Assume.assumeTrue(
            "Skipping getOfferings benchmark: no test store API key configured. " +
                "Set TEST_STORE_REVENUECAT_API_KEY (a test_-prefixed key) in local.properties or via -P.",
            apiKey.startsWith("test_"),
        )
    }

    @After
    fun tearDown() {
        Purchases.resetSingleton()
    }

    @Test
    fun getOfferingsMedianRemoteConfigDisabled() {
        runBenchmark(remoteConfigEnabled = false)
    }

    @Test
    fun getOfferingsMedianRemoteConfigEnabled() {
        runBenchmark(remoteConfigEnabled = true)
    }

    private fun runBenchmark(remoteConfigEnabled: Boolean) {
        val label = if (remoteConfigEnabled) "remote config ENABLED" else "remote config DISABLED"
        Log.i(
            TAG,
            "[$label] starting: $WARMUP_ITERATIONS warmup + $MEASURED_ITERATIONS measured cold iterations",
        )

        for (i in 1..WARMUP_ITERATIONS) {
            val ms = runSingleColdFetch(remoteConfigEnabled)
            Log.i(TAG, "[$label] warmup %d/%d: %.1f ms (discarded)".format(i, WARMUP_ITERATIONS, ms))
        }

        val samplesMs = mutableListOf<Double>()
        for (i in 1..MEASURED_ITERATIONS) {
            val ms = runSingleColdFetch(remoteConfigEnabled)
            samplesMs += ms
            val runningMedian = percentile(samplesMs.sorted(), MEDIAN_PERCENTILE)
            Log.i(
                TAG,
                "[$label] iteration %d/%d: %.1f ms (running median %.1f ms)".format(
                    i,
                    MEASURED_ITERATIONS,
                    ms,
                    runningMedian,
                ),
            )
        }

        val sorted = samplesMs.sorted()
        Log.i(
            TAG,
            "[$label] DONE over ${sorted.size} cold iterations (ms): " +
                "median=%.1f p90=%.1f min=%.1f max=%.1f mean=%.1f".format(
                    percentile(sorted, MEDIAN_PERCENTILE),
                    percentile(sorted, P90_PERCENTILE),
                    sorted.first(),
                    sorted.last(),
                    sorted.average(),
                ),
        )
    }

    /**
     * One cold-install iteration: tear down any previous instance, wipe on-disk state, configure fresh,
     * and time a single getOfferings call. Returns the elapsed time in milliseconds.
     */
    private fun runSingleColdFetch(remoteConfigEnabled: Boolean): Double {
        Purchases.resetSingleton()
        wipeDisk()
        configureOnMain(remoteConfigEnabled)
        return measureGetOfferingsMillis()
    }

    private fun measureGetOfferingsMillis(): Double {
        val latch = CountDownLatch(1)
        val errorRef = AtomicReference<PurchasesError?>()
        val elapsedNanos = AtomicLong(0L)
        val start = System.nanoTime()
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                elapsedNanos.set(System.nanoTime() - start)
                latch.countDown()
            }

            override fun onError(error: PurchasesError) {
                errorRef.set(error)
                latch.countDown()
            }
        })
        check(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "getOfferings timed out after $TIMEOUT_SECONDS s" }
        errorRef.get()?.let { throw AssertionError("getOfferings failed: $it") }
        return elapsedNanos.get() / NANOS_PER_MILLI
    }

    private fun configureOnMain(remoteConfigEnabled: Boolean) {
        // configure must run on the main thread: it registers a ProcessLifecycleOwner observer.
        // No appUserID is set, so each cold run gets a fresh anonymous id and a genuinely cold fetch.
        instrumentation.runOnMainSync {
            val builder = PurchasesConfiguration.Builder(context, apiKey)
            if (remoteConfigEnabled) {
                builder.dangerousSettings(DangerousSettings.forWorkflows())
            }
            Purchases.configure(builder.build())
        }
    }

    /**
     * Wipes everything the SDK persists so the next configure behaves like a clean install: the four
     * RevenueCat SharedPreferences files and the remote-config files under noBackupFilesDir/RevenueCat.
     */
    private fun wipeDisk() {
        listOf(
            RevenueCatBackupAgent.REVENUECAT_PREFS_FILE_NAME,
            "%s_preferences_etags".format(context.packageName),
            "com_revenuecat_purchases_%s_preferences_diagnostics".format(context.packageName),
            "com.revenuecat.purchases.transaction_metadata.%s".format(apiKey),
        ).forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
        File(context.noBackupFilesDir, "RevenueCat").deleteRecursively()
    }

    /**
     * Linear-interpolation percentile over an already-sorted list. p50 is the median.
     */
    private fun percentile(sorted: List<Double>, percentile: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val rank = (percentile / PERCENT_MAX) * (sorted.size - 1)
        val lowerIndex = floor(rank).toInt()
        val upperIndex = ceil(rank).toInt()
        val weight = rank - lowerIndex
        return sorted[lowerIndex] * (1 - weight) + sorted[upperIndex] * weight
    }

    private companion object {
        private const val TAG = "GetOfferingsBenchmark"
        private const val MEASURED_ITERATIONS = 25
        private const val WARMUP_ITERATIONS = 2
        private const val TIMEOUT_SECONDS = 15L
        private const val NANOS_PER_MILLI = 1_000_000.0
        private const val MEDIAN_PERCENTILE = 50.0
        private const val P90_PERCENTILE = 90.0
        private const val PERCENT_MAX = 100.0
    }
}
