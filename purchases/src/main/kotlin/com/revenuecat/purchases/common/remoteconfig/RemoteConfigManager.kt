package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.Backend
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

internal class RemoteConfigManager(
    private val backend: Backend,
    private val topicFetcher: TopicFetcher,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    fun updateRemoteConfigIfNeeded(
        appInBackground: Boolean,
        completion: ((PurchasesError?) -> Unit)? = null,
    ) {
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
        // WIP: We should have some logic to pick the correct source for this. Right now, hardcoded to the first source.
        val source = response.blobSources.firstOrNull()
        val tasks = response.manifest.topics.mapNotNull { (topic, entries) ->
            val entry = entries[DEFAULT_ENTRY_ID] ?: return@mapNotNull null
            TopicTask(topic, DEFAULT_ENTRY_ID, entry)
        }
        return if (source == null || tasks.isEmpty()) {
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
    }

    private suspend fun getRemoteConfig(appInBackground: Boolean): RemoteConfigResponse =
        suspendCancellableCoroutine { cont ->
            backend.getRemoteConfig(
                appInBackground = appInBackground,
                onSuccess = { cont.safeResume(it) },
                onError = { cont.safeResumeWithException(PurchasesException(it)) },
            )
        }

    private data class TopicTask(val topic: Topic, val entryId: String, val entry: TopicEntry)

    private companion object {
        const val DEFAULT_ENTRY_ID = "default"
    }
}
