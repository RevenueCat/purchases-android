package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.networking.HTTPTimeoutManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSourceHandle.Purpose
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.utils.DefaultUrlConnectionFactory
import com.revenuecat.purchases.utils.UrlConnection
import com.revenuecat.purchases.utils.UrlConnectionFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.ByteBuffer
import java.util.PriorityQueue

/** The placeholder a blob source URL template carries; substituted with the blob ref before download. */
private const val BLOB_REF_PLACEHOLDER = "{blob_ref}"

/**
 * Downloads remote-config blobs by their content-address ref and stores them in the **same** on-disk cache
 * inline blobs use ([RemoteConfigBlobStore]). It is the single primitive behind both background prefetch and
 * on-demand reads:
 *
 * - **Bounded concurrency**: a fixed pool of [MAX_CONCURRENT_DOWNLOADS] worker coroutines downloads at most that
 *   many blobs at once — the worker count *is* the cap (no semaphore needed).
 * - **Priority**: workers drain a priority queue, so an on-demand [ensureDownloaded] (HIGH) is served ahead of any
 *   backlog of [prefetch] (LOW) work. An on-demand request for a ref still queued as a prefetch **raises** its
 *   priority so it is picked next rather than stranded behind the backlog.
 * - **Dedupe**: a ref already queued or in flight is joined, not downloaded twice; every joiner gets the same
 *   result. A foreground prefetch and an explicit read of the same blob share one download.
 * - **Failure tolerant**: nothing thrown to callers; a failed or un-verifiable blob just yields `false` (it is
 *   recoverable — re-fetched on demand or on the next sync).
 *
 * Each download is verified against its ref (truncated SHA-256, URL-safe base64 — see
 * [RemoteConfigUtils.contentAddressRef], matching `RCElement.checksumBase64`) before it is stored, so a tampered
 * or corrupt payload is never cached.
 */
internal class RemoteConfigBlobFetcher(
    private val blobStore: RemoteConfigBlobStore,
    private val sourceProvider: RemoteConfigSourceProvider,
    private val timeoutManager: HTTPTimeoutManager,
    private val urlConnectionFactory: UrlConnectionFactory = DefaultUrlConnectionFactory(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    /** Scheduling priority. Natural (ordinal) order is used by the queue, so HIGH must sort after LOW. */
    private enum class Priority { LOW, HIGH }

    private class PendingDownload(
        val ref: String,
        var priority: Priority,
        var seq: Long,
        val awaiters: MutableList<CompletableDeferred<Boolean>> = mutableListOf(),
    )

    private val lock = Any()

    /** Every ref that is queued or in flight, for dedupe/boost. Kept until the download completes. */
    private val pending = HashMap<String, PendingDownload>()

    /** Refs a worker is currently downloading (a raise can no longer reorder these). */
    private val inFlight = HashSet<String>()

    /** Queued, not-yet-claimed downloads, ordered by priority (HIGH first) then enqueue order (FIFO). */
    // The (initialCapacity, comparator) constructor is API 1; the comparator-only overload is API 24+.
    private val queue = PriorityQueue<PendingDownload>(
        QUEUE_INITIAL_CAPACITY,
        compareByDescending<PendingDownload> { it.priority }.thenBy { it.seq },
    )

    private var seqCounter = 0L

    /** One token per queued item; idle workers suspend on it (no thread held) until work arrives. */
    private val signal = Channel<Unit>(Channel.UNLIMITED)

    init {
        repeat(MAX_CONCURRENT_DOWNLOADS) { scope.launch { runWorker() } }
    }

    /**
     * Ensures the blob for [ref] is in the store, downloading it if needed. Returns whether it ended up cached.
     * Cached → immediate `true`; otherwise the request is scheduled at HIGH priority and deduped against any
     * queued/in-flight download for the same ref.
     */
    suspend fun ensureDownloaded(ref: String): Boolean {
        if (!RemoteConfigUtils.isValidRef(ref)) {
            errorLog { "Refusing to download remote config blob with malformed ref '$ref'." }
            return false
        }
        return if (blobStore.contains(ref)) {
            true
        } else {
            // An on-demand read is worth a fresh pass: if a prior cycle exhausted the blob sources, re-arm them
            // so a source that has since recovered is retried. No-op (and keeps failover progress) otherwise.
            sourceProvider.restartIfExhausted(Purpose.BLOB)
            enqueue(ref, Priority.HIGH).await()
        }
    }

    /**
     * Ensures every ref in [refs] is in the store, downloading missing ones concurrently at HIGH priority.
     * Returns whether they *all* ended up cached. Deduping and per-ref validation are inherited from the
     * single-ref [ensureDownloaded].
     */
    suspend fun ensureDownloaded(refs: List<String>): Boolean = coroutineScope {
        refs.map { async { ensureDownloaded(it) } }.awaitAll().all { it }
    }

    /** Best-effort background warm of [refs] at LOW priority. Fire-and-forget; failures are tolerated. */
    fun prefetch(refs: List<String>) {
        scope.launch {
            var enqueued = 0
            refs.forEach { ref ->
                if (RemoteConfigUtils.isValidRef(ref) && !blobStore.contains(ref)) {
                    enqueue(ref, Priority.LOW)
                    enqueued++
                }
            }
            verboseLog { "Enqueued $enqueued remote config blob(s) for prefetch." }
        }
    }

    private fun enqueue(ref: String, priority: Priority): CompletableDeferred<Boolean> = synchronized(lock) {
        val deferred = CompletableDeferred<Boolean>()
        val existing = pending[ref]
        if (existing != null) {
            existing.awaiters.add(deferred)
            // Raising priority only matters while the ref is still queued; an in-flight download can't be reordered.
            if (priority > existing.priority && ref !in inFlight) {
                queue.remove(existing)
                existing.priority = priority
                existing.seq = seqCounter++
                queue.offer(existing)
            }
            return deferred
        }
        val download = PendingDownload(ref, priority, seqCounter++).apply { awaiters.add(deferred) }
        pending[ref] = download
        queue.offer(download)
        signal.trySend(Unit)
        return deferred
    }

    private suspend fun runWorker() {
        while (true) {
            signal.receive()
            val download = claimNext() ?: continue
            // A stray Throwable (e.g. OOM from readBytes(), or a bug) must not strand awaiters on an unresolved
            // deferred nor silently kill this worker; log it (so it surfaces) and fail the blob as recoverable.
            val result = try {
                downloadAndStore(download.ref)
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                errorLog(e) { "Unexpected failure downloading remote config blob '${download.ref}'." }
                false
            }
            // The store is shared: a concurrent writer (e.g. inline extraction during a config sync) may have cached
            // the ref while our download failed. Report the truthful "is it cached now?" so a benign race isn't a
            // false negative. Short-circuits on success, so the extra stat only happens on the failure path.
            complete(download, result || blobStore.contains(download.ref))
        }
    }

    private fun claimNext(): PendingDownload? = synchronized(lock) {
        queue.poll()?.also { inFlight.add(it.ref) }
    }

    private fun complete(download: PendingDownload, result: Boolean) {
        val awaiters = synchronized(lock) {
            pending.remove(download.ref)
            inFlight.remove(download.ref)
            download.awaiters.toList()
        }
        awaiters.forEach { it.complete(result) }
    }

    private fun downloadAndStore(ref: String): Boolean {
        // Another worker (or a just-completed prefetch) may have cached it between scheduling and now.
        if (blobStore.contains(ref)) return true

        // Each SOURCE_UNHEALTHY failure falls over to the next source; once the provider has none left
        // (getCurrent == null) the operation fails and stops. We never restart here, so a backlog of downloads
        // can't each spin re-walking sources already known to be bad. Re-arming an exhausted provider happens at
        // the operation entry instead (on-demand ensureDownloaded, and the manager's per-sync prefetch), bounding
        // it to one re-arm per request/cycle.
        var handle = sourceProvider.getCurrent(Purpose.BLOB)
        var result: Boolean? = null
        while (handle != null && result == null) {
            val url = handle.url.replace(BLOB_REF_PLACEHOLDER, ref)
            when (tryDownloadVerifyStore(url, ref)) {
                DownloadOutcome.SUCCESS -> result = true
                DownloadOutcome.BLOB_UNAVAILABLE -> result = false
                DownloadOutcome.SOURCE_UNHEALTHY -> {
                    sourceProvider.reportUnhealthy(handle)
                    handle = sourceProvider.getCurrent(Purpose.BLOB)
                    verboseLog {
                        "Remote config blob source unhealthy for '$ref'; " +
                            (handle?.let { "falling over to the next source." } ?: "no sources left.")
                    }
                }
            }
        }
        return result ?: false
    }

    private fun tryDownloadVerifyStore(url: String, ref: String): DownloadOutcome {
        val host = hostOf(url)
        val timeout = timeoutManager.getTimeoutForRequest(
            host = host,
            isFallback = false,
            endpointSupportsFallbackURLs = false,
            isProxied = false,
        ).toInt()

        var connection: UrlConnection? = null
        return try {
            connection = urlConnectionFactory.createConnection(url, timeout, timeout)
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    timeoutManager.recordRequestResult(host, HTTPTimeoutManager.RequestResult.SUCCESS_ON_MAIN_BACKEND)
                    val bytes = connection.inputStream.use { it.readBytes() }
                    verifyAndStore(bytes, ref, url)
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    errorLog { "Remote config blob '$ref' not found at $url (404)." }
                    DownloadOutcome.BLOB_UNAVAILABLE
                }
                else -> {
                    errorLog { "HTTP $code downloading remote config blob '$ref' from $url." }
                    DownloadOutcome.SOURCE_UNHEALTHY
                }
            }
        } catch (e: SocketTimeoutException) {
            errorLog(e) { "Timed out downloading remote config blob '$ref' from $url." }
            timeoutManager.recordRequestResult(host, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)
            DownloadOutcome.SOURCE_UNHEALTHY
        } catch (e: IOException) {
            errorLog(e) { "Failed to download remote config blob '$ref' from $url." }
            DownloadOutcome.SOURCE_UNHEALTHY
        } finally {
            connection?.disconnect()
        }
    }

    private fun hostOf(url: String): String? = try {
        URL(url).host
    } catch (e: MalformedURLException) {
        verboseLog { "Could not resolve host for remote config blob URL $url: ${e.message}" }
        null
    }

    private fun verifyAndStore(bytes: ByteArray, ref: String, url: String): DownloadOutcome {
        if (RemoteConfigUtils.contentAddressRef(bytes) != ref) {
            errorLog { "Remote config blob '$ref' from $url failed content-address verification." }
            return DownloadOutcome.BLOB_UNAVAILABLE
        }
        // A failed disk write isn't a source problem, so don't condemn the source: report the blob as unavailable
        // (yields false, stops) and let a later sync/on-demand read re-fetch it.
        return if (blobStore.write(ref, ByteBuffer.wrap(bytes))) {
            verboseLog { "Downloaded and stored remote config blob '$ref' (${bytes.size} bytes) from $url." }
            DownloadOutcome.SUCCESS
        } else {
            DownloadOutcome.BLOB_UNAVAILABLE
        }
    }

    private enum class DownloadOutcome { SUCCESS, SOURCE_UNHEALTHY, BLOB_UNAVAILABLE }

    private companion object {
        private const val MAX_CONCURRENT_DOWNLOADS = 4

        // Matches java.util.PriorityQueue's own default; it grows as needed.
        private const val QUEUE_INITIAL_CAPACITY = 11
    }
}
