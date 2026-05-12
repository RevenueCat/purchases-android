package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.caching.isCacheStale
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.safeResume
import com.revenuecat.purchases.common.safeResumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import kotlin.random.Random

@OptIn(InternalRevenueCatAPI::class)
@Suppress("LongParameterList")
internal class RemoteConfigManager(
    private val backend: Backend,
    private val topicFetcher: TopicFetcher,
    private val diskCache: RemoteConfigDiskCache,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val random: Random = Random.Default,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    @Volatile
    private var cache: CacheEntry? = null

    @Volatile
    private var cachedSourceId: String? = null

    fun updateRemoteConfigIfNeeded(
        appInBackground: Boolean,
        completion: ((PurchasesError?) -> Unit)? = null,
    ) {
        if (!cache?.cachedAt.isCacheStale(appInBackground, dateProvider)) {
            completion?.invoke(null)
            return
        }
        scope.launch {
            val error = refresh(appInBackground)
            completion?.invoke(error)
        }
    }

    private suspend fun refresh(appInBackground: Boolean): PurchasesError? {
        val response = try {
            getRemoteConfig(appInBackground)
        } catch (e: PurchasesException) {
            errorLog { "Failed to fetch remote config: ${e.error}" }
            return e.error
        }
        val referenced = buildReferenceSet(response.manifest)
        val tasks = response.manifest.topics.mapNotNull { (topic, entries) ->
            val entry = entries[DEFAULT_ENTRY_ID] ?: return@mapNotNull null
            TopicTask(topic, DEFAULT_ENTRY_ID, entry)
        }
        val outcome = if (response.blobSources.isEmpty() || tasks.isEmpty()) {
            RefreshOutcome.VacuousSuccess
        } else {
            downloadTopicsWithFallback(response.blobSources, tasks)
        }

        return when (outcome) {
            is RefreshOutcome.Success -> {
                cachedSourceId = outcome.source.id
                val previousResponse = cache?.response
                topicFetcher.cleanupUnreferencedTopics(referenced)
                cache = CacheEntry(response, dateProvider.now)
                if (previousResponse != response) {
                    diskCache.write(response)
                }
                null
            }
            RefreshOutcome.VacuousSuccess -> null
            is RefreshOutcome.Failure -> {
                if (outcome.clearCachedSource) {
                    cachedSourceId = null
                }
                outcome.error
            }
        }
    }

    private suspend fun downloadTopicsWithFallback(
        blobSources: List<BlobSource>,
        tasks: List<TopicTask>,
    ): RefreshOutcome {
        val triedIds = mutableSetOf<String>()
        var remaining = tasks
        var lastError: PurchasesError? = null
        var cachedSourceFailedInvalidating = false
        val previouslyCachedSourceId = cachedSourceId

        while (remaining.isNotEmpty()) {
            val source = pickNextSource(blobSources, triedIds, previouslyCachedSourceId) ?: break
            triedIds += source.id

            val attempt = attemptDownloads(source, remaining)
            lastError = lastError ?: attempt.firstError
            if (attempt.invalidating && source.id == previouslyCachedSourceId) {
                cachedSourceFailedInvalidating = true
            }
            if (attempt.stillFailing.isEmpty()) {
                return RefreshOutcome.Success(source)
            }
            remaining = attempt.stillFailing
        }

        return RefreshOutcome.Failure(
            error = lastError ?: PurchasesError(
                PurchasesErrorCode.NetworkError,
                "All blob sources exhausted while downloading topics.",
            ),
            clearCachedSource = cachedSourceFailedInvalidating,
        )
    }

    private suspend fun attemptDownloads(
        source: BlobSource,
        tasks: List<TopicTask>,
    ): SourceAttempt {
        val results = coroutineScope {
            tasks.map { task ->
                async {
                    task to topicFetcher.fetchTopicIfNeeded(
                        topic = task.topic,
                        entryId = task.entryId,
                        topicEntry = task.entry,
                        source = source,
                    )
                }
            }.awaitAll()
        }
        val stillFailing = mutableListOf<TopicTask>()
        var firstError: PurchasesError? = null
        var invalidating = false
        for ((task, result) in results) {
            when (result) {
                is TopicFetchResult.Success -> Unit
                is TopicFetchResult.TransientFailure -> {
                    stillFailing += task
                    firstError = firstError ?: result.error
                }
                is TopicFetchResult.InvalidatingFailure -> {
                    stillFailing += task
                    firstError = firstError ?: result.error
                    invalidating = true
                }
            }
        }
        return SourceAttempt(stillFailing, firstError, invalidating)
    }

    private fun pickNextSource(
        blobSources: List<BlobSource>,
        triedIds: Set<String>,
        previouslyCachedSourceId: String?,
    ): BlobSource? {
        if (triedIds.isEmpty() && previouslyCachedSourceId != null) {
            val cachedSource = blobSources.firstOrNull { it.id == previouslyCachedSourceId }
            if (cachedSource != null) return cachedSource
        }
        return blobSources.selectWeightedExcluding(triedIds, BlobSource::id, random)
    }

    private suspend fun getRemoteConfig(appInBackground: Boolean): RemoteConfigResponse =
        suspendCancellableCoroutine { cont ->
            backend.getRemoteConfig(
                appInBackground = appInBackground,
                onSuccess = { cont.safeResume(it) },
                onError = { cont.safeResumeWithException(PurchasesException(it)) },
            )
        }

    private fun buildReferenceSet(manifest: Manifest): Map<Topic, Set<String>> =
        manifest.topics.mapValues { (_, entries) ->
            entries.values.map { it.blobRef }.toSet()
        }

    private data class TopicTask(val topic: Topic, val entryId: String, val entry: TopicEntry)

    private data class SourceAttempt(
        val stillFailing: List<TopicTask>,
        val firstError: PurchasesError?,
        val invalidating: Boolean,
    )

    private data class CacheEntry(val response: RemoteConfigResponse, val cachedAt: Date)

    private sealed class RefreshOutcome {
        data class Success(val source: BlobSource) : RefreshOutcome()
        object VacuousSuccess : RefreshOutcome()
        data class Failure(val error: PurchasesError, val clearCachedSource: Boolean) : RefreshOutcome()
    }

    private companion object {
        const val DEFAULT_ENTRY_ID = "default"
    }
}
