package com.revenuecat.perftester

import android.content.Context
import android.os.Build
import android.os.Process
import android.os.SystemClock
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.getOfferingsWith
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.coroutines.resume

@OptIn(InternalRevenueCatAPI::class)
internal class PerfCycleRunner(
    private val context: Context,
    private val apiKey: String,
) {

    private val resultsFile = File(context.getExternalFilesDir(null), "results.jsonl")
    private val writer = ResultsWriter(resultsFile)

    val resultsFilePath: String get() = resultsFile.absolutePath

    @Suppress("LongMethod")
    suspend fun runCycle(workflowsEnabled: Boolean, pressIndex: Int): PerfResult {
        if (Purchases.isConfigured) {
            Purchases.sharedInstance.close()
        }
        val hadOfferings = SdkCaches.hadOfferingsCache(context, apiKey)
        val hadRemoteConfig = SdkCaches.hadRemoteConfigCache(context)
        val hadBlobs = SdkCaches.hadBlobsCache(context)
        val processStartToPressMs = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()

        val dangerousSettings = if (workflowsEnabled) {
            DangerousSettings.forWorkflows()
        } else {
            DangerousSettings()
        }
        val configuration = PurchasesConfiguration.Builder(context, apiKey)
            .dangerousSettings(dangerousSettings)
            .build()

        val t0 = SystemClock.elapsedRealtimeNanos()
        Purchases.configure(configuration)
        val t1 = SystemClock.elapsedRealtimeNanos()

        var t2 = 0L
        var errorMessage: String? = null
        var offerings: Offerings? = null
        val timedOut = runCatching {
            withTimeout(CALL_TIMEOUT_MILLIS) {
                suspendCancellableCoroutine { continuation ->
                    Purchases.sharedInstance.getOfferingsWith(
                        onError = { error ->
                            t2 = SystemClock.elapsedRealtimeNanos()
                            errorMessage = error.toString()
                            continuation.resume(Unit)
                        },
                        onSuccess = { received ->
                            t2 = SystemClock.elapsedRealtimeNanos()
                            offerings = received
                            continuation.resume(Unit)
                        },
                    )
                }
            }
        }.isFailure
        if (timedOut && t2 == 0L) {
            errorMessage = "getOfferings did not complete within ${CALL_TIMEOUT_MILLIS / MILLIS_PER_SECOND}s"
        }

        val configureToOfferingsMs = if (t2 != 0L) (t2 - t0).nanosToMillis() else null
        val getOfferingsMs = if (t2 != 0L) (t2 - t1).nanosToMillis() else null
        val success = errorMessage == null && offerings != null

        var uiConfigMs: Double? = null
        var uiConfigIsDefault: Boolean? = null
        var workflowResolutionMs: Double? = null
        var workflowBodyMs: Double? = null
        var workflowResolved: Boolean? = null
        if (workflowsEnabled && success) {
            runCatching {
                withTimeout(CALL_TIMEOUT_MILLIS) {
                    val started = SystemClock.elapsedRealtimeNanos()
                    val uiConfig = Purchases.sharedInstance.awaitGetUiConfig()
                    uiConfigMs = (SystemClock.elapsedRealtimeNanos() - started).nanosToMillis()
                    uiConfigIsDefault = uiConfig == UiConfig()
                }
            }
            val offeringIds = offerings?.let { received ->
                listOfNotNull(received.current?.identifier) +
                    received.all.keys.filter { it != received.current?.identifier }
            }.orEmpty()
            var workflowId: String? = null
            runCatching {
                withTimeout(CALL_TIMEOUT_MILLIS) {
                    val started = SystemClock.elapsedRealtimeNanos()
                    workflowId = offeringIds.firstNotNullOfOrNull { offeringId ->
                        Purchases.sharedInstance.workflowIdForOfferingId(offeringId)
                    }
                    workflowResolutionMs = (SystemClock.elapsedRealtimeNanos() - started).nanosToMillis()
                }
            }
            workflowId?.let { id ->
                // NOTE: this withTimeout cannot force-cancel awaitGetWorkflow — that SDK API uses
                // non-cancellable suspendCoroutine, so a hung workflow-body fetch will keep runCycle
                // waiting past CALL_TIMEOUT_MILLIS. The offerings measurement above is properly bounded.
                workflowResolved = runCatching {
                    withTimeout(CALL_TIMEOUT_MILLIS) {
                        val started = SystemClock.elapsedRealtimeNanos()
                        Purchases.sharedInstance.awaitGetWorkflow(id)
                        workflowBodyMs = (SystemClock.elapsedRealtimeNanos() - started).nanosToMillis()
                    }
                }.isSuccess
            }
        }

        val result = PerfResult(
            timestampEpochMs = System.currentTimeMillis(),
            sdkVersion = Purchases.frameworkVersion,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidApiLevel = Build.VERSION.SDK_INT,
            workflowsEnabled = workflowsEnabled,
            pressIndexInProcess = pressIndex,
            processStartToPressMs = processStartToPressMs,
            hadOfferingsCache = hadOfferings,
            hadRemoteConfigCache = hadRemoteConfig,
            hadBlobsCache = hadBlobs,
            configureToOfferingsMs = configureToOfferingsMs,
            getOfferingsMs = getOfferingsMs,
            uiConfigMs = uiConfigMs,
            uiConfigIsDefault = uiConfigIsDefault,
            workflowResolutionMs = workflowResolutionMs,
            workflowBodyMs = workflowBodyMs,
            workflowResolved = workflowResolved,
            success = success,
            errorMessage = errorMessage,
            offeringsCount = offerings?.all?.size,
            currentOfferingId = offerings?.current?.identifier,
        )
        writer.append(result)
        return result
    }

    private fun Long.nanosToMillis(): Double = this / NANOS_PER_MILLI

    private companion object {
        private const val CALL_TIMEOUT_MILLIS = 120_000L
        private const val MILLIS_PER_SECOND = 1_000L
        private const val NANOS_PER_MILLI = 1_000_000.0
    }
}
