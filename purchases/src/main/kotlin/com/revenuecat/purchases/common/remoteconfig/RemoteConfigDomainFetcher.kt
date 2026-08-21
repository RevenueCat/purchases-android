package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.GetRemoteConfigErrorHandlingBehavior
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.common.warnLog
import kotlinx.coroutines.CompletableDeferred
import java.util.Date

/**
 * The suspend bridge between a tree pass and the callback-based [Backend] config endpoints: issues one domain's
 * `/v1/config` request (or the root's fallback request) and classifies the answer. Holds none of the manager's
 * commit state — it only reads each domain's persisted request bookkeeping and the blob store's held refs.
 */
internal class RemoteConfigDomainFetcher(
    private val backend: Backend,
    private val diskCache: RemoteConfigDiskCache,
    private val blobStore: RemoteConfigBlobStore,
    /**
     * Invoked at classification (callback) time on a root/fallback 4xx — even when the requesting pass was
     * cancelled meanwhile, since a late response is still a valid signal that the endpoint refuses this app.
     */
    private val onRootClientError: (PurchasesError) -> Unit,
) {
    internal enum class FetchOutcome { Success, FailedRetryable, ClientError }

    internal class DomainFetchResult(
        val outcome: FetchOutcome,
        /** The `200` payload; null on a `204` and on failures. */
        val container: RCContainer? = null,
        val requestDate: Date? = null,
    )

    /**
     * Issues the `/v1/config` request for [domain], replaying that domain's own persisted sync bookkeeping (the
     * opaque manifest, the last successful refresh time, and the prefetch blobs actually held), and suspends
     * until the backend answers.
     */
    suspend fun fetch(
        domain: String,
        isRoot: Boolean,
        appInBackground: Boolean,
        appUserID: String,
        fetchContext: RemoteConfigFetchContext,
    ): DomainFetchResult {
        val domainState = diskCache.read()?.domains?.get(domain)
        val storedBlobs = blobStore.cachedRefs()
        verboseLog {
            "Refreshing remote config (domain=$domain, manifest present=${domainState?.manifest != null}, " +
                "appInBackground=$appInBackground)."
        }
        val result = CompletableDeferred<DomainFetchResult>()
        backend.getRemoteConfig(
            appInBackground = appInBackground,
            appUserID = appUserID,
            fetchContext = fetchContext,
            domain = domain,
            // Opaque manifest replayed verbatim; null on this domain's first sync.
            manifest = domainState?.manifest,
            lastRefreshTime = domainState?.lastRefreshTime?.let(::Date),
            // Report only the prefetch blobs we actually hold, so the server stops re-inlining them.
            prefetchedBlobs = domainState?.prefetchBlobs?.filter { storedBlobs.contains(it) } ?: emptyList(),
            onSuccess = { container, requestDate, _ ->
                if (requestDate == null) {
                    // Not fatal — the previous value carries forward — but it means the server stopped telling
                    // us its own time, so the refresh time replayed on later requests is frozen. Surfaced here
                    // rather than swallowed because nothing else makes this observable in the field.
                    warnLog {
                        "Remote config response carried no ${HTTPResult.REQUEST_TIME_HEADER_NAME} header. " +
                            "Keeping the previous refresh time; the server cannot see how fresh this client's " +
                            "configuration is."
                    }
                }
                result.complete(DomainFetchResult(FetchOutcome.Success, container, requestDate))
            },
            onError = { error, behavior ->
                result.complete(DomainFetchResult(classifyFailure(error, behavior, isRoot)))
            },
        )
        return result.await()
    }

    /** The root's cold-start fallback request (plain JSON, no container); null when it fails. */
    suspend fun fetchFallback(appInBackground: Boolean, domain: String): RemoteConfiguration? {
        val result = CompletableDeferred<RemoteConfiguration?>()
        backend.getRemoteConfigFallback(
            appInBackground = appInBackground,
            domain = domain,
            onSuccess = { response, _ -> result.complete(response) },
            onError = { error, behavior ->
                classifyFailure(error, behavior, isRoot = true)
                result.complete(null)
            },
        )
        return result.await()
    }

    private fun classifyFailure(
        error: PurchasesError,
        behavior: GetRemoteConfigErrorHandlingBehavior,
        isRoot: Boolean,
    ): FetchOutcome = if (behavior == GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE) {
        if (isRoot) {
            onRootClientError(error)
        } else {
            log(LogIntent.RC_ERROR) { "Remote config subdomain request was refused (4xx). Error: $error" }
        }
        FetchOutcome.ClientError
    } else {
        errorLog(error)
        FetchOutcome.FailedRetryable
    }
}
