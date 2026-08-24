@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.common.uiconfig.UiConfigResolution
import com.revenuecat.purchases.common.verboseLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The consumer-facing entry point for reading workflows. It stays as the seam the SDK calls (so
 * `PurchasesOrchestrator` and the public API are unchanged), but it is now a thin adapter that reads from the
 * `/v1/config` layer through [WorkflowsConfigProvider] instead of calling the backend per workflow.
 *
 * Everything the old backend-backed path owned is gone: there is no per-workflow `Backend.getWorkflow` call, no
 * CDN envelope resolution, no `WorkflowsCache`, no stale-while-revalidate, and no disk-fallback. Freshness comes
 * from the shared config sync ([com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager]); bodies are
 * read — or downloaded on demand and deduped — by that manager.
 *
 * `appUserID` has dropped out of the read entirely: a workflow body is a shared, content-addressed blob, not a
 * per-user document. (Per-user data — A/B `enrolled_variants` — is being designed separately.)
 */
@Suppress("TooManyFunctions")
internal class WorkflowManager(
    private val workflowsConfigProvider: WorkflowsConfigProvider,
    private val uiConfigProvider: UiConfigProvider,
    private val workflowAssetPrewarmer: WorkflowAssetPrewarmer,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    // Guards [inFlightReadiness] so that concurrent calls to [onPaywallConfigReady] see the same
    // Deferred and don't start a second round of readiness work while one is already in flight.
    private val readinessLock = Any()

    // The single in-flight readiness check started on [scope]. Null when no check is in progress.
    // Written only under [readinessLock]; read under the lock to decide whether to reuse or start.
    @Volatile
    private var inFlightReadiness: Deferred<Unit>? = null

    fun close() {
        scope.cancel()
    }

    /**
     * Resolves a workflow by workflow id or offering id, or throws [PurchasesException] when it cannot be
     * served. Memory-first: on a warm cache every read below resumes synchronously (no dispatch), so a caller
     * on a `Dispatchers.Main.immediate` coroutine gets the workflow without ever suspending. The workflow's
     * images and `ui_config` fonts are pre-downloaded fire-and-forget on [scope]; a prewarm failure never fails
     * the read.
     */
    suspend fun getWorkflow(workflowOrOfferingId: String): PublishedWorkflow {
        // An offering id resolves to its workflow id up front from persisted config metadata; a workflow id
        // passes through. No backend round-trip, no lazy offering→workflow conversion.
        val workflowId = workflowsConfigProvider.workflowIdForOfferingId(workflowOrOfferingId)
            ?: workflowOrOfferingId
        val workflow = getWorkflowBody(workflowId)
        val uiConfig = loadUiConfig(workflowId)
            ?: throw PurchasesException(
                PurchasesError(
                    PurchasesErrorCode.UnknownError,
                    "Workflow '$workflowId' resolved, but its UI config is unavailable.",
                ),
            )

        // Warm the workflow's images and ui_config fonts in parallel with delivery, like the old fetch path did
        // on resolve; a prewarm failure must never fail the read itself. The fonts now come from the ui_config
        // topic rather than a uiConfig embedded in the workflow body.
        prewarmWorkflowAssets(workflow, uiConfig)
        return workflow
    }

    /** Prewarms presentation assets without changing delivery or failing the caller. */
    fun prewarmWorkflowAssets(workflow: PublishedWorkflow, uiConfig: UiConfig) {
        scope.launch {
            runCatching {
                workflowAssetPrewarmer.preDownloadWorkflowAssets(workflow, uiConfig)
            }.onFailure { errorLog(it) { "Failed to pre-download workflow assets" } }
        }
    }

    /**
     * Loads a published workflow body by its workflow id without resolving offering aliases, loading UI config,
     * or prewarming presentation assets. Checkpoint resolution uses this to inspect the workflow kind before it
     * decides which additional dependencies, if any, are needed to serve it.
     */
    suspend fun getWorkflowBody(workflowId: String): PublishedWorkflow =
        workflowsConfigProvider.getWorkflow(workflowId)
            ?: throw PurchasesException(
                PurchasesError(
                    PurchasesErrorCode.UnknownError,
                    "Workflow '$workflowId' is unavailable from remote config.",
                ),
            )

    /** Loads `ui_config` (memory-first), swallowing non-cancellation failures to null so the caller decides. */
    private suspend fun loadUiConfig(workflowId: String): UiConfig? =
        try {
            uiConfigProvider.getUiConfig()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { "Failed to load ui_config for workflow '$workflowId'." }
            null
        }

    suspend fun resolveWorkflow(offeringId: String): WorkflowResolution =
        workflowsConfigProvider.resolveWorkflow(offeringId)

    /** See [WorkflowsConfigProvider.offeringIdByWorkflowId]. */
    suspend fun offeringIdByWorkflowId(): Map<String, String> =
        workflowsConfigProvider.offeringIdByWorkflowId()

    /**
     * Invokes [onComplete] once the config-endpoint paywall data `getOfferings` depends on is ready — the
     * config-topic replacement for the old `getWorkflowsList(onComplete=)` gate `OfferingsManager` used to
     * preserve the guarantee that `getOfferings` doesn't return until that data is ready to be queried.
     *
     * "Ready" means, resolved concurrently:
     * - the workflows topic is synced, so [workflowIdForOfferingId] resolves; and
     * - the `ui_config` body is resolved, so a workflow render — and, once `ui_config`/paywall components stop
     *   shipping inline in `/offerings` and move to the config endpoint, the offering itself — has its styling
     *   in hand without a further round-trip. A project with no paywalls configured publishes no `ui_config` at
     *   all, which is a valid ready outcome rather than a failure (see [UiConfigResolution]).
     *
     * Both steps are best-effort: a failure to ready either one is swallowed (logged) and never propagates,
     * so [onComplete] always runs and `getOfferings` can never be stranded waiting on it — if config data
     * couldn't be readied, the render path fetches it on demand and reports an error if it is still unavailable.
     * The only thing that skips [onComplete] is coroutine cancellation (teardown / identity change), which must not
     * fire a spurious success. A failure in one step also does not cancel the other.
     *
     * **Fast path:** when the in-memory caches already hold what the current offering needs, [onComplete] runs
     * **synchronously** on the caller's thread (no `scope.launch`), so a warm `awaitOfferings()` resumes without
     * a thread hop — the whole point of gating here rather than dispatching unconditionally.
     *
     * Otherwise the caches are warmed on [scope] and [onComplete] fires when done. Overlapping calls are
     * coalesced onto one [Deferred]. `ui_config` is resolved first: it is memory-first and self-primes a
     * `/v1/config` sync on a cold cache, which also commits the workflows topic, so warming the workflow parsed
     * cache next reads committed data.
     */
    fun onPaywallConfigReady(onComplete: () -> Unit) {
        // warm() may already have run with no current offering (config commits before offerings land), and the
        // fast path below would then never re-run it. Deduped by workflow id, so a repeat call is free.
        workflowsConfigProvider.prewarmOfferingAssets()
        if (uiConfigProvider.isWarm() && workflowsConfigProvider.isWarmForCurrentOffering()) {
            onComplete()
            return
        }
        val readiness: Deferred<Unit> = synchronized(readinessLock) {
            inFlightReadiness ?: scope.async {
                awaitBestEffort("ui_config") { readyUiConfig() }
                awaitBestEffort("workflows") { workflowsConfigProvider.warm() }
            }.also { deferred ->
                inFlightReadiness = deferred
                deferred.invokeOnCompletion {
                    synchronized(readinessLock) {
                        if (inFlightReadiness === deferred) {
                            inFlightReadiness = null
                        }
                    }
                }
            }
        }
        scope.launch {
            readiness.await()
            onComplete()
        }
    }

    /**
     * Resolves `ui_config` for the gate and reports the outcome at the level it deserves. Only a topic that
     * actually publishes ui_config parts and still can't be resolved is a failure: a project with no paywalls
     * configured has no `ui_config` at all, and error-logging that was pure noise.
     */
    private suspend fun readyUiConfig() {
        when (uiConfigProvider.resolveUiConfig()) {
            is UiConfigResolution.Found -> Unit
            UiConfigResolution.NotConfigured -> verboseLog {
                "Remote config has no ui_config to resolve before getOfferings."
            }
            UiConfigResolution.Disabled -> debugLog {
                "Remote config is disabled for this session; skipping ui_config readiness before getOfferings."
            }
            UiConfigResolution.Superseded -> verboseLog {
                "ui_config was superseded by a newer config commit before getOfferings; the next read re-resolves it."
            }
            UiConfigResolution.Unavailable -> errorLog { readinessFailureMessage("ui_config") }
        }
    }

    /**
     * Runs a best-effort readiness step: swallows and logs any failure so it can't strand the gate, but lets
     * [kotlinx.coroutines.CancellationException] propagate so real cancellation still tears the gate down
     * instead of being reported as a completed step. (Plain `runCatching` would swallow cancellation too.)
     */
    private suspend fun awaitBestEffort(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { readinessFailureMessage(what) }
        }
    }

    private fun readinessFailureMessage(what: String) =
        "Failed to ready $what before getOfferings; proceeding without it."
}
