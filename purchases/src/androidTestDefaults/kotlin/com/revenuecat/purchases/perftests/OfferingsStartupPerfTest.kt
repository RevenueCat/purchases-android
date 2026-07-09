package com.revenuecat.purchases.perftests

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.MainActivity
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.backup.RevenueCatBackupAgent
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.Config
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.configure
import com.revenuecat.purchases.factories.StoreProductFactory
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.resetSingleton
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Startup offerings performance harness: measures how long `getOfferings` takes across the workflows
 * (`/v1/config`) migration, under controlled scenario x cache-state combinations. Network conditions are
 * shaped *outside* this test (emulator/`tc netem`) and recorded via `PERF_TEST_NETWORK_LABEL`.
 *
 * This is not a pass/fail test: it always succeeds as long as it can run, and writes one JSON result file per
 * run to `getExternalFilesDir()/revenuecat-perf/` for `scripts/perf/run_offerings_startup_perf.py` to pull
 * and aggregate. It self-skips when `PERF_TEST_API_KEY` is not provided, so it is inert in regular
 * `connectedAndroidTest` runs.
 *
 * Billing is mocked: a `StoreProduct` is fabricated for every product id the offerings response references,
 * so Play Store variance is excluded from the measurement and the measurement isolates SDK + backend + network
 * behavior. See `PerfTestConfig` for the argument reference.
 */
@Suppress("TooManyFunctions")
internal class OfferingsStartupPerfTest {

    @get:Rule
    var activityScenarioRule = activityScenarioRule<MainActivity>()

    private lateinit var config: PerfTestConfig
    private lateinit var context: Context
    private lateinit var diagnosticsReader: DiagnosticsEventsReader

    private val runStartedAtEpochMs = System.currentTimeMillis()
    private val userIdBase = "android-perf-test-$runStartedAtEpochMs"

    @Before
    fun setUp() {
        config = PerfTestConfig.fromInstrumentationArguments()
        assumeTrue(
            "Skipping perf test: PERF_TEST_API_KEY instrumentation argument not provided.",
            config.apiKey.isNotEmpty(),
        )
        context = InstrumentationRegistry.getInstrumentation().targetContext
        diagnosticsReader = DiagnosticsEventsReader(context)
        Constants.proxyUrl.takeIf { it.isNotEmpty() }?.let { Purchases.proxyURL = URL(it) }
    }

    @Test
    fun measureOfferingsStartupPerformance() {
        val iterations = (0 until config.iterations).map { iteration ->
            runIteration(iteration).also {
                Log.i(TAG, "Iteration $iteration: ${it.toJson()}")
            }
        }
        Purchases.resetSingleton()
        val result = PerfRunResult(
            config = config,
            sdkVersion = Config.frameworkVersion,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidApiLevel = Build.VERSION.SDK_INT,
            startedAtEpochMs = runStartedAtEpochMs,
            iterations = iterations,
        )
        val outputFile = writeResult(result)
        Log.i(TAG, "Perf run complete. Results written to: ${outputFile.absolutePath}")
    }

    // region iteration lifecycle

    private fun runIteration(iteration: Int): PerfIterationResult {
        val userId = "$userIdBase-$iteration"
        val configureTimed = prepareCacheState(userId)
        // Drain diagnostics events produced during priming so the timed window only sees its own requests.
        diagnosticsReader.harvestNewHttpEvents()
        return measureTimedPhase(iteration, userId, configureTimed)
    }

    /**
     * Puts caches in the state [PerfCacheMode] describes and returns whether the timed phase must configure
     * the SDK itself (false only for [PerfCacheMode.WARM_MEMORY], which reuses the primed instance).
     */
    private fun prepareCacheState(userId: String): Boolean {
        Purchases.resetSingleton()
        clearAllPersistentState()
        return when (config.cacheMode) {
            PerfCacheMode.COLD -> true
            PerfCacheMode.WARM_DISK -> {
                primeCaches(userId)
                Purchases.resetSingleton()
                true
            }
            PerfCacheMode.WARM_MEMORY -> {
                primeCaches(userId)
                false
            }
            PerfCacheMode.WARM_OFFERINGS_COLD_CONFIG -> {
                primeCaches(userId)
                Purchases.resetSingleton()
                deleteRemoteConfigState()
                true
            }
        }
    }

    @Suppress("LongMethod", "ReturnCount")
    private fun measureTimedPhase(iteration: Int, userId: String, configureTimed: Boolean): PerfIterationResult {
        var configureStartedNanos: Long? = null
        if (configureTimed) {
            configureStartedNanos = SystemClock.elapsedRealtimeNanos()
            configureSdk(userId)
        }

        val latch = CountDownLatch(1)
        // Written on the SDK callback thread, read after latch.await() (which provides the happens-before).
        var offeringsCallbackNanos = 0L
        var errorMessage: String? = null
        var offeringsCount: Int? = null
        var currentOfferingId: String? = null
        var offeringIds: List<String> = emptyList()

        val getOfferingsStartedNanos = SystemClock.elapsedRealtimeNanos()
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                offeringsCallbackNanos = SystemClock.elapsedRealtimeNanos()
                errorMessage = error.toString()
                latch.countDown()
            },
            onSuccess = { offerings ->
                offeringsCallbackNanos = SystemClock.elapsedRealtimeNanos()
                offeringsCount = offerings.all.size
                currentOfferingId = offerings.current?.identifier
                offeringIds = listOfNotNull(offerings.current?.identifier) +
                    offerings.all.keys.filter { it != offerings.current?.identifier }
                latch.countDown()
            },
        )
        val finished = latch.await(TIMED_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!finished) {
            return PerfIterationResult(
                iteration = iteration,
                success = false,
                errorMessage = "getOfferings did not complete within ${TIMED_CALL_TIMEOUT_SECONDS}s",
                httpEvents = harvestHttpEvents(),
            )
        }

        val getOfferingsMs = (offeringsCallbackNanos - getOfferingsStartedNanos).nanosToMillis()
        val configureToOfferingsMs = configureStartedNanos?.let {
            (offeringsCallbackNanos - it).nanosToMillis()
        }
        if (errorMessage != null) {
            return PerfIterationResult(
                iteration = iteration,
                success = false,
                errorMessage = errorMessage,
                configureToOfferingsMs = configureToOfferingsMs,
                getOfferingsMs = getOfferingsMs,
                httpEvents = harvestHttpEvents(),
            )
        }

        val renderReadiness = if (config.scenario.useWorkflows) measureRenderReadiness(offeringIds) else null
        return PerfIterationResult(
            iteration = iteration,
            success = true,
            configureToOfferingsMs = configureToOfferingsMs,
            getOfferingsMs = getOfferingsMs,
            offeringsCount = offeringsCount,
            currentOfferingId = currentOfferingId,
            uiConfigMs = renderReadiness?.uiConfigMs,
            uiConfigIsDefault = renderReadiness?.uiConfigIsDefault,
            workflowId = renderReadiness?.workflowId,
            workflowResolutionMs = renderReadiness?.workflowResolutionMs,
            workflowBodyMs = renderReadiness?.workflowBodyMs,
            workflowResolved = renderReadiness?.workflowResolved,
            httpEvents = harvestHttpEvents(),
        )
    }

    // endregion

    // region render readiness (workflows scenarios)

    private data class RenderReadinessResult(
        val uiConfigMs: Double?,
        val uiConfigIsDefault: Boolean?,
        val workflowId: String?,
        val workflowResolutionMs: Double?,
        val workflowBodyMs: Double?,
        val workflowResolved: Boolean?,
    )

    /**
     * Measures how ready the *render* path is once `getOfferings` has returned: `ui_config` resolution (with
     * default-fallback detection — the all-or-nothing merge silently degrades to `UiConfig()` on failure) and
     * the time to resolve + load the first available workflow body. Under the current gate, blob-backed
     * bodies are not guaranteed ready at `getOfferings` time, so this is a co-headline metric.
     */
    private fun measureRenderReadiness(offeringIds: List<String>): RenderReadinessResult = runBlocking {
        var uiConfigMs: Double? = null
        var uiConfigIsDefault: Boolean? = null
        runCatching {
            withTimeout(POST_MEASUREMENT_TIMEOUT_MILLIS) {
                val started = SystemClock.elapsedRealtimeNanos()
                val uiConfig = Purchases.sharedInstance.awaitGetUiConfig()
                uiConfigMs = (SystemClock.elapsedRealtimeNanos() - started).nanosToMillis()
                uiConfigIsDefault = uiConfig == UiConfig()
            }
        }

        var workflowId: String? = null
        var workflowResolutionMs: Double? = null
        runCatching {
            withTimeout(POST_MEASUREMENT_TIMEOUT_MILLIS) {
                val started = SystemClock.elapsedRealtimeNanos()
                workflowId = offeringIds.firstNotNullOfOrNull { offeringId ->
                    Purchases.sharedInstance.workflowIdForOfferingId(offeringId)
                }
                workflowResolutionMs = (SystemClock.elapsedRealtimeNanos() - started).nanosToMillis()
            }
        }

        var workflowBodyMs: Double? = null
        var workflowResolved: Boolean? = null
        workflowId?.let { id ->
            workflowResolved = runCatching {
                withTimeout(POST_MEASUREMENT_TIMEOUT_MILLIS) {
                    val started = SystemClock.elapsedRealtimeNanos()
                    Purchases.sharedInstance.awaitGetWorkflow(id)
                    workflowBodyMs = (SystemClock.elapsedRealtimeNanos() - started).nanosToMillis()
                }
            }.isSuccess
        }

        RenderReadinessResult(
            uiConfigMs = uiConfigMs,
            uiConfigIsDefault = uiConfigIsDefault,
            workflowId = workflowId,
            workflowResolutionMs = workflowResolutionMs,
            workflowBodyMs = workflowBodyMs,
            workflowResolved = workflowResolved,
        )
    }

    // endregion

    // region SDK configuration

    private fun configureSdk(userId: String) {
        val billing = buildBillingMock()
        val dangerousSettings = if (config.scenario.useWorkflows) {
            DangerousSettings.forWorkflows()
        } else {
            DangerousSettings()
        }
        val configuration = PurchasesConfiguration.Builder(context, config.apiKey)
            .appUserID(userId)
            .diagnosticsEnabled(true)
            .dangerousSettings(dangerousSettings)
            .build()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Purchases.configure(configuration, billing, config.scenario.forceServerErrorStrategy())
        }
    }

    /** Untimed full warm-up: offerings + (on workflows scenarios) ui_config and the first workflow body. */
    private fun primeCaches(userId: String) {
        configureSdk(userId)
        runBlocking {
            val offerings = withTimeout(PRIME_TIMEOUT_MILLIS) { Purchases.sharedInstance.awaitOfferings() }
            if (config.scenario.useWorkflows) {
                runCatching {
                    withTimeout(PRIME_TIMEOUT_MILLIS) { Purchases.sharedInstance.awaitGetUiConfig() }
                }
                runCatching {
                    withTimeout(PRIME_TIMEOUT_MILLIS) {
                        val workflowId = offerings.all.keys.firstNotNullOfOrNull {
                            Purchases.sharedInstance.workflowIdForOfferingId(it)
                        }
                        workflowId?.let { Purchases.sharedInstance.awaitGetWorkflow(it) }
                    }
                }
            }
        }
    }

    /**
     * A `BillingAbstract` that fabricates a subscription `StoreProduct` for every product id the offerings
     * response requests. For Play Store projects, the Google offering parser matches packages by
     * (productId, basePlanId), so a product is fabricated per configured base plan id
     * (`PERF_TEST_BASE_PLAN_IDS`); Test Store projects match by product id alone.
     */
    private fun buildBillingMock(): BillingAbstract {
        val basePlanIds = config.basePlanIds.ifEmpty { listOf(DEFAULT_BASE_PLAN_ID) }
        return mockk<BillingAbstract>(relaxed = true).apply {
            every { isConnected() } returns true
            every { queryPurchases(any(), onSuccess = any(), onError = any()) } answers {
                secondArg<(Map<String, StoreTransaction>) -> Unit>().invoke(emptyMap())
            }
            every { queryAllPurchases(any(), any(), any()) } answers {
                secondArg<(List<StoreTransaction>) -> Unit>().invoke(emptyList())
            }
            every { queryProductDetailsAsync(any(), any(), any(), any()) } answers {
                val productType = firstArg<ProductType>()
                val productIds = secondArg<Set<String>>()
                val onReceive = thirdArg<StoreProductsCallback>()
                val products = if (productType == ProductType.SUBS) {
                    productIds.flatMap { productId -> fabricateProducts(productId, basePlanIds) }
                } else {
                    emptyList()
                }
                onReceive(products)
            }
        }
    }

    private fun fabricateProducts(productId: String, basePlanIds: List<String>): List<StoreProduct> =
        basePlanIds.map { basePlanId ->
            StoreProductFactory.createGoogleStoreProduct(
                productId = productId,
                basePlanId = basePlanId,
                subscriptionOptionsList = listOf(
                    StoreProductFactory.createGoogleSubscriptionOption(
                        productId = productId,
                        basePlanId = basePlanId,
                    ),
                ),
            )
        }

    // endregion

    // region cache surgery

    private fun clearAllPersistentState() {
        clearSharedPreferences(RevenueCatBackupAgent.REVENUECAT_PREFS_FILE_NAME)
        clearSharedPreferences("${context.packageName}_preferences_etags")
        clearSharedPreferences("com_revenuecat_purchases_${context.packageName}_preferences_diagnostics")
        deleteRemoteConfigState()
        File(context.filesDir, "RevenueCat").deleteRecursively()
        diagnosticsReader.reset()
    }

    private fun deleteRemoteConfigState() {
        // Mirrors RemoteConfigDiskCache and RemoteConfigBlobStore locations.
        File(File(context.noBackupFilesDir, "RevenueCat"), "remote_config").deleteRecursively()
        File(File(context.noBackupFilesDir, "RevenueCat"), "blobs").deleteRecursively()
    }

    private fun clearSharedPreferences(name: String) {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
    }

    // endregion

    // region output

    private fun harvestHttpEvents(): List<HttpRequestEvent> {
        // Diagnostics events are appended asynchronously on the events dispatcher; give them a moment to flush.
        SystemClock.sleep(DIAGNOSTICS_FLUSH_WAIT_MILLIS)
        return diagnosticsReader.harvestNewHttpEvents()
    }

    private fun writeResult(result: PerfRunResult): File {
        val outputDir = File(context.getExternalFilesDir(null), OUTPUT_DIR_NAME).apply { mkdirs() }
        val fileName = listOf(
            config.scenario.wireName,
            config.cacheMode.wireName,
            config.networkLabel.sanitizeForFileName(),
            runStartedAtEpochMs.toString(),
        ).joinToString("__") + ".json"
        val outputFile = File(outputDir, fileName)
        outputFile.writeText(result.toJson().toString(JSON_INDENT_SPACES))
        return outputFile
    }

    private fun String.sanitizeForFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "-")

    // endregion

    private fun Long.nanosToMillis(): Double = this / NANOS_PER_MILLI

    private companion object {
        private const val TAG = "RCPerfTest"
        private const val OUTPUT_DIR_NAME = "revenuecat-perf"
        private const val DEFAULT_BASE_PLAN_ID = "default"
        private const val TIMED_CALL_TIMEOUT_SECONDS = 120L
        private const val PRIME_TIMEOUT_MILLIS = 120_000L
        private const val POST_MEASUREMENT_TIMEOUT_MILLIS = 120_000L
        private const val DIAGNOSTICS_FLUSH_WAIT_MILLIS = 500L
        private const val NANOS_PER_MILLI = 1_000_000.0
        private const val JSON_INDENT_SPACES = 2
    }
}
