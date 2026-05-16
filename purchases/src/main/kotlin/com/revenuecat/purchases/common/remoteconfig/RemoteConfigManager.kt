package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
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
        val source = response.blobSources.selectWeighted(random)
        val referenced = buildReferenceSet(response.manifest)
        val tasks = response.manifest.topics.mapNotNull { (topic, entries) ->
            val entry = entries[DEFAULT_ENTRY_ID] ?: return@mapNotNull null
            TopicTask(topic, DEFAULT_ENTRY_ID, entry)
        }
        val firstError: PurchasesError? = if (source == null || tasks.isEmpty()) {
            null
        } else {
            coroutineScope {
                tasks.map { task ->
                    async {
                        topicFetcher.fetchTopicIfNeeded(
                            topic = task.topic,
                            entryId = task.entryId,
                            topicEntry = task.entry,
                            source = source,
                        )
                    }
                }.awaitAll().firstNotNullOfOrNull { it }
            }
        }
        if (firstError == null) {
            // Only cache when at least one topic was actually fetched — empty sources, empty topics,
            // and missing default entryIds are treated as no-op refreshes that don't populate the cache.
            if (source != null && tasks.isNotEmpty()) {
                val previousResponse = cache?.response
                topicFetcher.cleanupUnreferencedTopics(referenced)
                cache = CacheEntry(response, dateProvider.now)
                if (previousResponse != response) {
                    diskCache.write(response)
                }
            }
        }
        return firstError
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

    private data class CacheEntry(val response: RemoteConfigResponse, val cachedAt: Date)

    private companion object {
        const val DEFAULT_ENTRY_ID = "default"
    }
}
